package com.ptit.cvrecruiter.matching;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SynonymDictionaryTest {

    private final SynonymDictionary dictionary = new SynonymDictionary();

    @Test
    void databaseExpandsToVietnameseSynonyms() {
        List<String> variants = dictionary.expand("Database");
        assertTrue(variants.contains("co so du lieu"));
        assertTrue(variants.contains("csdl"));
    }

    @Test
    void reverseLookupFindsCanonicalGroup() {
        List<String> variants = dictionary.expand("cơ sở dữ liệu");
        assertTrue(variants.contains("database"));
    }

    @Test
    void unknownSkillFallsBackToItself() {
        List<String> variants = dictionary.expand("Kỹ năng đàm phán");
        assertTrue(variants.contains("ky nang dam phan"));
    }
}
