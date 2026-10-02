package com.nxoim.caif.core

import com.nxoim.caif.utils.typeSetMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypeSetMapTest {

    interface CapabilityA
    interface CapabilityB
    class ImplA1 : CapabilityA
    class ImplA2 : CapabilityA
    class ImplB1 : CapabilityB

    @Test
    fun givenTypeSetMap_whenAddingMultipleInstancesOfSameType_thenAllAreStoredInSet() {
        val setMap = typeSetMap()
        val a1 = ImplA1()
        val a2 = ImplA2()
        val b1 = ImplB1()

        setMap.put<CapabilityA>(a1)
        setMap.put<CapabilityA>(a2)
        setMap.put<CapabilityB>(b1)

        val setA = setMap.get<CapabilityA>()
        assertEquals(2, setA.size)
        assertTrue(a1 in setA)
        assertTrue(a2 in setA)

        val setB = setMap.get<CapabilityB>()
        assertEquals(1, setB.size)
        assertTrue(b1 in setB)

        val setUnmapped = setMap.get<String>()
        assertTrue(setUnmapped.isEmpty())
    }

    @Test
    fun givenTypeSetMap_whenRemovingInstance_thenOnlyThatInstanceIsRemoved() {
        val setMap = typeSetMap()
        val a1 = ImplA1()
        val a2 = ImplA2()

        setMap.put<CapabilityA>(a1)
        setMap.put<CapabilityA>(a2)

        assertTrue(setMap.remove<CapabilityA>(a1))
        assertFalse(setMap.remove<CapabilityA>(a1))

        val setA = setMap.get<CapabilityA>()
        assertEquals(1, setA.size)
        assertFalse(a1 in setA)
        assertTrue(a2 in setA)
    }

    @Test
    fun givenTypeSetMap_whenIteratingForEachEntry_thenAllEntriesAreVisited() {
        val setMap = typeSetMap()
        val a1 = ImplA1()
        val a2 = ImplA2()
        val b1 = ImplB1()

        setMap.put<CapabilityA>(a1)
        setMap.put<CapabilityA>(a2)
        setMap.put<CapabilityB>(b1)

        val visited = mutableListOf<Pair<Any, Any>>()
        setMap.forEachEntry { type, instance ->
            visited += (type to instance)
        }

        assertEquals(3, visited.size)
        assertTrue((CapabilityA::class to a1) in visited)
        assertTrue((CapabilityA::class to a2) in visited)
        assertTrue((CapabilityB::class to b1) in visited)
    }

    private data class EqualCapability(val id: Int) : CapabilityA

    @Test
    fun givenTypeSetMap_whenAddingDuplicateInstances_thenKeepsOneEqualEntry() {
        val setMap = typeSetMap()
        val capability = EqualCapability(1)

        setMap.put<CapabilityA>(capability)
        setMap.put<CapabilityA>(capability)
        setMap.put<CapabilityA>(EqualCapability(1))

        val stored = setMap.get<CapabilityA>()
        assertEquals(1, stored.size)
        assertTrue(capability in stored)
        assertTrue(EqualCapability(1) in stored)
    }
}
