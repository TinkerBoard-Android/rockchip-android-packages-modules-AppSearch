/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;
import android.os.Parcel;

import androidx.test.filters.SdkSuppress;

import org.junit.Test;

public class ParcelableUtilUnitTest {
    private static final int SMALL_BLOB_10_KB = 10_000;
    private static final int LARGE_BLOB_10_MB = 10_000_000;

    @Test
    public void testReadWriteSmallBlob() {
        final byte[] expectedBytes = fillData(new byte[SMALL_BLOB_10_KB]);
        Parcel parcel = Parcel.obtain();
        ParcelableUtil.writeBlob(parcel, expectedBytes);
        parcel.setDataPosition(0);
        byte[] actualBytes = ParcelableUtil.readBlob(parcel);
        assertThat(actualBytes).isEqualTo(expectedBytes);
        parcel.recycle();
    }

    @Test
    public void testReadWriteLargeBlob() {
        final byte[] expectedBytes = fillData(new byte[LARGE_BLOB_10_MB]);
        Parcel parcel = Parcel.obtain();
        ParcelableUtil.writeBlob(parcel, expectedBytes);
        parcel.setDataPosition(0);
        byte[] actualBytes = ParcelableUtil.readBlob(parcel);
        assertThat(actualBytes).isEqualTo(expectedBytes);
        parcel.recycle();
    }

    private static byte[] fillData(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 100);
        }
        return data;
    }
}
