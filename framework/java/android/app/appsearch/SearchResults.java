/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.app.appsearch;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.util.Preconditions;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * SearchResults are a returned object from a query API.
 *
 * <p>Each {@link SearchResult} contains a document and may contain other fields like snippets
 * based on request.
 *
 * <p>Should close this object after finish fetching results.
 *
 * <p>This class is not thread safe.
 */
public class SearchResults implements Closeable {
    private static final String TAG = "SearchResults";

    private final IAppSearchManager mService;

    @Nullable
    private final String mDatabaseName;

    private final String mQueryExpression;

    private final SearchSpec mSearchSpec;

    private final Executor mExecutor;

    private long mNextPageToken;

    private boolean mIsFirstLoad = true;

    SearchResults(@NonNull IAppSearchManager service,
            @Nullable String databaseName,
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec,
            @NonNull @CallbackExecutor Executor executor) {
        mService = Preconditions.checkNotNull(service);
        mExecutor = Preconditions.checkNotNull(executor);
        mDatabaseName = databaseName;
        mQueryExpression = Preconditions.checkNotNull(queryExpression);
        mSearchSpec = Preconditions.checkNotNull(searchSpec);
    }

    /**
     * Gets a whole page of {@link SearchResult}s.
     *
     * <p>Re-call this method to get next page of {@link SearchResult}, until it returns an
     * empty list.
     *
     * <p>The page size is set by {@link SearchSpec.Builder#setResultCountPerPage}.
     *
     * @param callback Callback to receive the pending result of performing this operation.
     */
    public void getNextPage(@NonNull Consumer<AppSearchResult<List<SearchResult>>> callback) {
        try {
            if (mIsFirstLoad) {
                mIsFirstLoad = false;
                if (mDatabaseName == null) {
                    mService.globalQuery(mQueryExpression, mSearchSpec.getBundle(),
                            wrapCallback(callback));
                } else {
                    mService.query(mDatabaseName, mQueryExpression, mSearchSpec.getBundle(),
                            wrapCallback(callback));
                }
            } else {
                mService.getNextPage(mNextPageToken, wrapCallback(callback));
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void invokeCallback(AppSearchResult result,
            @NonNull Consumer<AppSearchResult<List<SearchResult>>> callback) {
        if (result.isSuccess()) {
            try {
                SearchResultPage searchResultPage =
                        new SearchResultPage((Bundle) result.getResultValue());
                mNextPageToken = searchResultPage.getNextPageToken();
                callback.accept(AppSearchResult.newSuccessfulResult(
                        searchResultPage.getResults()));
            } catch (Throwable t) {
                callback.accept(AppSearchResult.throwableToFailedResult(t));
            }
        } else {
            callback.accept(result);
        }
    }
    @Override
    public void close() {
        mExecutor.execute(() -> {
            try {
                mService.invalidateNextPageToken(mNextPageToken);
            } catch (RemoteException e) {
                Log.d(TAG, "Unable to close the SearchResults", e);
            }
        });
    }

    private IAppSearchResultCallback wrapCallback(
            @NonNull Consumer<AppSearchResult<List<SearchResult>>> callback) {
        return new IAppSearchResultCallback.Stub() {
            public void onResult(AppSearchResult result) {
                mExecutor.execute(() -> invokeCallback(result, callback));
            }
        };
    }
}
