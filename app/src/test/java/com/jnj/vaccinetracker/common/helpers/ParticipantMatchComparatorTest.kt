package com.jnj.vaccinetracker.common.helpers

import com.jnj.vaccinetracker.common.domain.entities.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ParticipantMatchComparatorTest : FunSpec({

    fun createComparator(criteria: ParticipantIdentificationCriteria) = ParticipantMatchComparator(criteria)
    fun createMatch(uuid: String = uuid(), participantId: String, phone: String?, matchingScore: Int? = null) =
        ParticipantMatch(uuid, participantId, matchingScore, Gender.FEMALE, BirthDate.Companion.yearOfBirth(1994), null, emptyMap<String, String>().withPhone(phone))

    context("without biometrics") {
        fun createCriteria(participantId: String, phone: String?) = ParticipantIdentificationCriteria(participantId, phone, null)
        val matches =
            listOf(
                createMatch(participantId = "1", phone = "0123"),
                createMatch(participantId = "2", phone = "321"),
                createMatch(participantId = "3", phone = "1234"),
            )
        test("and no matches then list should remain unchanged") {
            val criteria = createCriteria("__", "__")
            val sortedList = matches.sortedWith(createComparator(criteria))
            sortedList shouldBe matches
        }
        test("and participantId match then put it first") {
            val criteria = createCriteria("2", "__")
            val sortedList = matches.sortedWith(createComparator(criteria))
            sortedList.map { it.participantId } shouldBe listOf("2", "1", "3")
        }

        test("and phone match then put it first") {
            val criteria = createCriteria("__", "1234")
            val sortedList = matches.sortedWith(createComparator(criteria))
            sortedList.map { it.participantId } shouldBe listOf("3", "1", "2")
        }

        test("and both phone & participantId match (different participants) then put participantId match first, phone match second") {
            val criteria = createCriteria("2", "1234")
            val sortedList = matches.sortedWith(createComparator(criteria))
            sortedList.map { it.participantId } shouldBe listOf("2", "3", "1")
        }
    }
    context("with biometrics") {
        fun createCriteria(participantId: String, phone: String?) = ParticipantIdentificationCriteria(participantId, phone, BiometricsTemplateBytes(byteArrayOf(1)))
        val matches =
            listOf(createMatch(participantId = "1", phone = "0123", matchingScore = 3),
                createMatch(participantId = "2", phone = "321", matchingScore = 10),
                createMatch(participantId = "3", phone = "1234", matchingScore = 5))
        test("and no matches then list should remain unchanged") {
            val dataSet = matches.map { it.copy(matchingScore = null) }
            val criteria = createCriteria("__", "__")
            val sortedList = dataSet.sortedWith(createComparator(criteria))
            sortedList shouldBe dataSet
        }
        test("and participantId match then put it first, afterwards sort according to match score") {
            val criteria = createCriteria("2", "__")
            val sortedList = matches.sortedWith(createComparator(criteria))
            sortedList.map { it.participantId } shouldBe listOf("2", "3", "1")
        }

        test("and phone match then put it first, afterwards sort according to match score") {
            val criteria = createCriteria("__", "1234")
            val sortedList = matches.sortedWith(createComparator(criteria))
            sortedList.map { it.participantId } shouldBe listOf("3", "2", "1")
        }

        test("and participantId match has different phone and null matching score then put participantId match first, phone matches second, sort according to match score") {
            val dataSet =
                matches + listOf(createMatch(participantId = "4", phone = "1234", matchingScore = 1), createMatch(participantId = "5", phone = "133", matchingScore = null))
            val criteria = createCriteria("5", "1234")
            val sortedList = dataSet.sortedWith(createComparator(criteria))
            sortedList.map { it.participantId } shouldBe listOf("5", "3", "4", "2", "1")
        }

        test("and participantId match has no phone and null matching score then put participantId match first, phone matches second, sort according to match score") {
            val dataSet =
                matches + listOf(createMatch(participantId = "4", phone = "1234", matchingScore = 1), createMatch(participantId = "5", phone = null, matchingScore = null))
            val criteria = createCriteria("5", "1234")
            val sortedList = dataSet.sortedWith(createComparator(criteria))
            sortedList.map { it.participantId } shouldBe listOf("5", "3", "4", "2", "1")
        }

        test("and participantId match has same phone and null matching score then put participantId match first, phone matches second, sort according to match score") {
            val dataSet =
                matches + listOf(createMatch(participantId = "4", phone = "1234", matchingScore = 1), createMatch(participantId = "5", phone = "1234", matchingScore = null))
            val criteria = createCriteria("5", "1234")
            val sortedList = dataSet.sortedWith(createComparator(criteria))
            sortedList.map { it.participantId } shouldBe listOf("5", "3", "4", "2", "1")
        }
    }

})
