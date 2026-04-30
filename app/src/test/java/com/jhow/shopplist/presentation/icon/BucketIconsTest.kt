package com.jhow.shopplist.presentation.icon

import com.jhow.shopplist.domain.icon.IconBucket
import org.junit.Assert.assertNotNull
import org.junit.Test

class BucketIconsTest {

    @Test
    fun `every icon bucket maps to a non-null image vector`() {
        IconBucket.entries.forEach { bucket ->
            assertNotNull(
                "Bucket $bucket should map to a non-null ImageVector",
                BucketIcons.forBucket(bucket)
            )
        }
    }
}
