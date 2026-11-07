Fixes issue where Geode can spawn inside a structure and break something https://bugs.mojang.com/browse/MC/issues/MC-205250

This mod just checks if Geode intersects with a structure and skips Geode generation if it does intersect. This should resolve this issue