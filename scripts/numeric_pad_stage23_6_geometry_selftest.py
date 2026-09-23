#!/usr/bin/env python3
import math

# Mirror the production KeySpec weights without importing Android/Kotlin classes.
bottom = [
    ("ABC", 1.5),
    (",", 1.0),
    ("!?#", 1.0),
    ("0", 2.0),
    ("=", 1.0),
    (".", 1.0),
    ("ENTER", 1.5),
]

total = sum(weight for _, weight in bottom)
assert math.isclose(total, 9.0), total

before_zero = 0.0
zero_weight = None
for label, weight in bottom:
    if label == "0":
        zero_weight = weight
        break
    before_zero += weight

assert zero_weight is not None
zero_center = (before_zero + zero_weight / 2.0) / total
assert math.isclose(zero_center, 0.5), zero_center

# Main geometry is 1.5 left + 6 center + 1.5 right. The middle digit column
# is at the center of the 6-unit center grid, therefore also 50% overall.
main_total = 1.5 + 6.0 + 1.5
eight_center = (1.5 + 3.0) / main_total
assert math.isclose(eight_center, 0.5), eight_center
assert math.isclose(zero_center, eight_center), (zero_center, eight_center)

print("STAGE 23.6 NUMERIC GEOMETRY SELF-TEST: PASS (4/4)")
print("- bottom width = 9 units")
print("- 0 center = 50%")
print("- 8 column center = 50%")
print("- 0 is horizontally aligned directly below 8")
