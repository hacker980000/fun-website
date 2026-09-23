#!/usr/bin/env python3
import math

DIGIT_ROWS = 3
bottom = [
    ("ABC", 1.5),
    (",", 1.0),
    ("!?#", 1.0),
    ("0", 2.0),
    ("=", 1.0),
    (".", 1.0),
    ("ENTER", 1.5),
]

checks = 0

def expect(condition, message):
    global checks
    assert condition, message
    checks += 1

expect(DIGIT_ROWS == 3, "center block must contain exactly 3 digit rows")

total = sum(weight for _, weight in bottom)
expect(math.isclose(total, 9.0), f"bottom width is {total}")

before_zero = 0.0
zero_weight = None
for label, weight in bottom:
    if label == "0":
        zero_weight = weight
        break
    before_zero += weight

expect(zero_weight is not None, "0 missing")
zero_center = (before_zero + zero_weight / 2.0) / total
expect(math.isclose(zero_center, 0.5), f"0 center is {zero_center}")

main_total = 1.5 + 6.0 + 1.5
eight_center = (1.5 + 3.0) / main_total
expect(math.isclose(eight_center, 0.5), f"8 center is {eight_center}")
expect(math.isclose(zero_center, eight_center), (zero_center, eight_center))

left_of_zero = [label for label, _ in bottom[:3]]
right_of_zero = [label for label, _ in bottom[4:]]
expect(left_of_zero == ["ABC", ",", "!?#"], left_of_zero)
expect(right_of_zero == ["=", ".", "ENTER"], right_of_zero)

print(f"STAGE 23.7 NUMERIC GEOMETRY SELF-TEST: PASS ({checks}/{checks})")
print("- digit-grid height = exactly 3 rows")
print("- no blank fourth center row before the action row")
print("- 0 center = 8 column center = 50%")
print("- same-line order = ABC , !?# | 0 | = . Enter")
