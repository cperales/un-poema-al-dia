import os
import random
import sys
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "lambda"))

import poem_of_the_day as pod  # noqa: E402


class ParseKeyTest(unittest.TestCase):
    def test_nested_poemas_prefix_is_dropped(self):
        key = "Poemas/Poemas/Antonio Machado/Anoche cuando dormía.md"
        self.assertEqual(
            pod.parse_key(key, "Poemas/"),
            ("Antonio Machado", "Anoche cuando dormía"),
        )

    def test_single_level_layout(self):
        key = "Poemas/Rosalía de Castro/Las campanas.md"
        self.assertEqual(pod.parse_key(key, "Poemas/"), ("Rosalía de Castro", "Las campanas"))

    def test_title_with_parentheses_and_empty_prefix(self):
        key = "Gustavo Adolfo Bécquer/Rimas (LIII).md"
        self.assertEqual(pod.parse_key(key, ""), ("Gustavo Adolfo Bécquer", "Rimas (LIII)"))


class PickTest(unittest.TestCase):
    KEYS = ["a.md", "b.md", "c.md"]

    def test_avoids_used(self):
        for seed in range(20):
            key, used = pod.pick(self.KEYS, ["a.md", "b.md"], random.Random(seed))
            self.assertEqual(key, "c.md")
            self.assertEqual(used, ["a.md", "b.md", "c.md"])

    def test_resets_when_all_used(self):
        key, used = pod.pick(self.KEYS, list(self.KEYS), random.Random(1))
        self.assertIn(key, self.KEYS)
        self.assertEqual(used, [key])

    def test_drops_deleted_poems_from_history(self):
        _, used = pod.pick(["a.md", "b.md"], ["gone.md"], random.Random(1))
        self.assertNotIn("gone.md", used)

    def test_no_poems_raises(self):
        with self.assertRaises(ValueError):
            pod.pick([], [])

    def test_every_poem_shown_once_per_cycle(self):
        rng, used, seen = random.Random(0), [], []
        for _ in self.KEYS:
            key, used = pod.pick(self.KEYS, used, rng)
            seen.append(key)
        self.assertEqual(sorted(seen), self.KEYS)


class PayloadTest(unittest.TestCase):
    def test_payload_shape(self):
        p = pod.build_payload("2026-11-17", "Poemas/X/Y.md", "Poemas/", "hola")
        self.assertEqual(
            p,
            {"date": "2026-11-17", "id": "Poemas/X/Y.md", "title": "Y", "author": "X", "text": "hola"},
        )


if __name__ == "__main__":
    unittest.main()
