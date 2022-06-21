package com.jnj.vaccinetracker.common.data.files

import com.jnj.vaccinetracker.common.helpers.deleteChildren
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

class ParticipantDataFileIOKtTest : FunSpec({

    test("deleteChildren") {
        // Arrange
        val folder = File("test123")
        folder.exists() shouldBe false

        try {
            folder.mkdir()
            (0.until(10)).forEach { n ->
                File(folder, "$n.txt").createNewFile()
            }
            folder.exists() shouldBe true
            folder.list()!!.size shouldBe 10
            // Act
            folder.deleteChildren()
            // Assert
            folder.exists() shouldBe true
            folder.list()!!.size shouldBe 0
        } finally {
            folder.deleteRecursively()
        }
    }
})
