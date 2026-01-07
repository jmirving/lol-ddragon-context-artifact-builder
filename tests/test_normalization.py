import unittest

from lol_ddragon_context_artifact_builder.champion_mapping import (
    build_champion_mapping,
    validate_mapping,
)
from lol_ddragon_context_artifact_builder.normalization import normalize_name


class NormalizeNameTests(unittest.TestCase):
    def test_normalizes_punctuation_and_spaces(self):
        self.assertEqual(normalize_name("Cho'Gath"), "chogath")
        self.assertEqual(normalize_name("Dr. Mundo"), "drmundo")
        self.assertEqual(normalize_name("Nunu & Willump"), "nunuwillump")

    def test_strips_diacritics(self):
        self.assertEqual(normalize_name("Café"), "cafe")


class ChampionMappingTests(unittest.TestCase):
    def test_builds_sorted_mapping(self):
        payload = {
            "data": {
                "Wukong": {"name": "Wukong", "id": "MonkeyKing", "key": "62"},
                "Ahri": {"name": "Ahri", "id": "Ahri", "key": "103"},
            }
        }

        mapping = build_champion_mapping(payload)
        validate_mapping(mapping)

        self.assertEqual(mapping[0]["name"], "Ahri")
        self.assertEqual(mapping[1]["name"], "Wukong")
        self.assertEqual(mapping[0]["normalized_name"], "ahri")
        self.assertEqual(mapping[1]["normalized_name"], "wukong")


if __name__ == "__main__":
    unittest.main()
