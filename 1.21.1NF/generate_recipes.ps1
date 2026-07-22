# Generate material-variant recipes for CreateLabor Villager NF 1.21.1
$recipeDir = "e:\git\Minecraft\MC MOD\CL CreateLabor Villager\1.21.1NF\src\main\resources\data\create_labor\recipe"

# Delete old recipe files (keep villager_binder.json)
Get-ChildItem $recipeDir -Filter "*_seat*.json" | Remove-Item -Force
Write-Host "Deleted old seat recipes"

# Helper: write JSON file
function WriteRecipe($name, $json) {
    [System.IO.File]::WriteAllText("$recipeDir\$name.json", $json)
}

# === ANDESITE VERSION (Task 2) ===
# Original recipes with empty slots filled with andesite_alloy

# --- Saw ---
# from_create_seat: "TZT" (no empty slots in 1x3, keep as-is)
WriteRecipe "andesite_saw_seat_from_create_seat" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"TZT`"],`"key`":{`"T`":{`"item`":`"create:andesite_alloy`"},`"Z`":{`"tag`":`"create:seats`"}},`"result`":{`"id`":`"create_labor:andesite_saw_seat`",`"count`":1}}"
# from_wool: "TYT", " M " -> "TYT", "AMA"
WriteRecipe "andesite_saw_seat_from_wool" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"TYT`",`"AMA`"],`"key`":{`"T`":{`"item`":`"create:andesite_alloy`"},`"Y`":{`"tag`":`"minecraft:wool`"},`"M`":{`"tag`":`"minecraft:wooden_slabs`"},`"A`":{`"item`":`"create:andesite_alloy`"}},`"result`":{`"id`":`"create_labor:andesite_saw_seat`",`"count`":1}}"
# mixing: seats + andesite_alloy (original already has andesite)
WriteRecipe "andesite_saw_seat_mixing" "{`"type`":`"create:mixing`",`"ingredients`":[{`"tag`":`"create:seats`"},{`"item`":`"create:andesite_alloy`"}],`"results`":[{`"id`":`"create_labor:andesite_saw_seat`",`"count`":1}]}"

# --- Press ---
# from_create_seat: " T ", " Z " -> "ATA", "AZA"
WriteRecipe "andesite_press_seat_from_create_seat" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"ATA`",`"AZA`"],`"key`":{`"T`":{`"item`":`"minecraft:iron_ingot`"},`"A`":{`"item`":`"create:andesite_alloy`"},`"Z`":{`"tag`":`"create:seats`"}},`"result`":{`"id`":`"create_labor:andesite_press_seat`",`"count`":1}}"
# from_wool: " T ", " Y ", " M " -> "ATA", "AYA", "AMA"
WriteRecipe "andesite_press_seat_from_wool" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"ATA`",`"AYA`",`"AMA`"],`"key`":{`"T`":{`"item`":`"minecraft:iron_ingot`"},`"A`":{`"item`":`"create:andesite_alloy`"},`"Y`":{`"tag`":`"minecraft:wool`"},`"M`":{`"tag`":`"minecraft:wooden_slabs`"}},`"result`":{`"id`":`"create_labor:andesite_press_seat`",`"count`":1}}"
# mixing: seats + iron_ingot + andesite_alloy
WriteRecipe "andesite_press_seat_mixing" "{`"type`":`"create:mixing`",`"ingredients`":[{`"tag`":`"create:seats`"},{`"item`":`"minecraft:iron_ingot`"},{`"item`":`"create:andesite_alloy`"}],`"results`":[{`"id`":`"create_labor:andesite_press_seat`",`"count`":1}]}"

# --- Mixer ---
# original: "BHB", " Z " -> "BHB", "AZA"
WriteRecipe "andesite_mixer_seat" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"BHB`",`"AZA`"],`"key`":{`"B`":{`"tag`":`"c:plates/iron`"},`"H`":{`"item`":`"create:andesite_alloy`"},`"A`":{`"item`":`"create:andesite_alloy`"},`"Z`":{`"tag`":`"create:seats`"}},`"result`":{`"id`":`"create_labor:andesite_mixer_seat`",`"count`":1}}"

# --- Millstone ---
# from_create_seat: " T ", " Z " -> "ATA", "AZA"
WriteRecipe "andesite_millstone_seat_from_create_seat" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"ATA`",`"AZA`"],`"key`":{`"T`":{`"item`":`"create:cogwheel`"},`"A`":{`"item`":`"create:andesite_alloy`"},`"Z`":{`"tag`":`"create:seats`"}},`"result`":{`"id`":`"create_labor:andesite_millstone_seat`",`"count`":1}}"
# from_wool: " T ", " Y ", " M " -> "ATA", "AYA", "AMA"
WriteRecipe "andesite_millstone_seat_from_wool" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"ATA`",`"AYA`",`"AMA`"],`"key`":{`"T`":{`"item`":`"create:cogwheel`"},`"A`":{`"item`":`"create:andesite_alloy`"},`"Y`":{`"tag`":`"minecraft:wool`"},`"M`":{`"tag`":`"c:stones`"}},`"result`":{`"id`":`"create_labor:andesite_millstone_seat`",`"count`":1}}"
# mixing: seats + cogwheel + andesite_alloy
WriteRecipe "andesite_millstone_seat_mixing" "{`"type`":`"create:mixing`",`"ingredients`":[{`"tag`":`"create:seats`"},{`"item`":`"create:cogwheel`"},{`"item`":`"create:andesite_alloy`"}],`"results`":[{`"id`":`"create_labor:andesite_millstone_seat`",`"count`":1}]}"

# --- Deployer ---
# from_create_seat: " T ", " Z " -> "ATA", "AZA"
WriteRecipe "andesite_deployer_seat_from_create_seat" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"ATA`",`"AZA`"],`"key`":{`"T`":{`"item`":`"create:brass_hand`"},`"A`":{`"item`":`"create:andesite_alloy`"},`"Z`":{`"tag`":`"create:seats`"}},`"result`":{`"id`":`"create_labor:andesite_deployer_seat`",`"count`":1}}"
# from_wool: " T ", " Y ", " M " -> "ATA", "AYA", "AMA"
WriteRecipe "andesite_deployer_seat_from_wool" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"ATA`",`"AYA`",`"AMA`"],`"key`":{`"T`":{`"item`":`"create:brass_hand`"},`"A`":{`"item`":`"create:andesite_alloy`"},`"Y`":{`"tag`":`"minecraft:wool`"},`"M`":{`"tag`":`"minecraft:wooden_slabs`"}},`"result`":{`"id`":`"create_labor:andesite_deployer_seat`",`"count`":1}}"
# mixing: seats + brass_hand + andesite_alloy
WriteRecipe "andesite_deployer_seat_mixing" "{`"type`":`"create:mixing`",`"ingredients`":[{`"tag`":`"create:seats`"},{`"item`":`"create:brass_hand`"},{`"item`":`"create:andesite_alloy`"}],`"results`":[{`"id`":`"create_labor:andesite_deployer_seat`",`"count`":1}]}"

Write-Host "Generated 13 andesite recipes"

# === COPPER VERSION (Task 3 - surround andesite seat with 8 copper_ingot) ===
$workstations = @("saw", "press", "mixer", "millstone", "deployer")
foreach ($ws in $workstations) {
    $name = "copper_${ws}_seat"
    $json = "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"CCC`",`"CXC`",`"CCC`"],`"key`":{`"C`":{`"item`":`"minecraft:copper_ingot`"},`"X`":{`"item`":`"create_labor:andesite_${ws}_seat`"}},`"result`":{`"id`":`"create_labor:$name`",`"count`":1}}"
    WriteRecipe $name $json
}
Write-Host "Generated 5 copper recipes"

# === BRASS VERSION (Task 3 - surround andesite seat with 8 brass_ingot) ===
foreach ($ws in $workstations) {
    $name = "brass_${ws}_seat_from_andesite"
    $json = "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"CCC`",`"CXC`",`"CCC`"],`"key`":{`"C`":{`"item`":`"create:brass_ingot`"},`"X`":{`"item`":`"create_labor:andesite_${ws}_seat`"}},`"result`":{`"id`":`"create_labor:brass_${ws}_seat`",`"count`":1}}"
    WriteRecipe $name $json
}
Write-Host "Generated 5 brass surround recipes"

# === BRASS DIRECT VERSION (Task 4 - replace andesite_alloy with brass_ingot in original recipes) ===

# --- Saw ---
WriteRecipe "brass_saw_seat_from_create_seat" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"TZT`"],`"key`":{`"T`":{`"item`":`"create:brass_ingot`"},`"Z`":{`"tag`":`"create:seats`"}},`"result`":{`"id`":`"create_labor:brass_saw_seat`",`"count`":1}}"
WriteRecipe "brass_saw_seat_from_wool" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"TYT`",`"AMA`"],`"key`":{`"T`":{`"item`":`"create:brass_ingot`"},`"Y`":{`"tag`":`"minecraft:wool`"},`"M`":{`"tag`":`"minecraft:wooden_slabs`"},`"A`":{`"item`":`"create:brass_ingot`"}},`"result`":{`"id`":`"create_labor:brass_saw_seat`",`"count`":1}}"
WriteRecipe "brass_saw_seat_mixing" "{`"type`":`"create:mixing`",`"ingredients`":[{`"tag`":`"create:seats`"},{`"item`":`"create:brass_ingot`"}],`"results`":[{`"id`":`"create_labor:brass_saw_seat`",`"count`":1}]}"

# --- Press ---
WriteRecipe "brass_press_seat_from_create_seat" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"ATA`",`"AZA`"],`"key`":{`"T`":{`"item`":`"minecraft:iron_ingot`"},`"A`":{`"item`":`"create:brass_ingot`"},`"Z`":{`"tag`":`"create:seats`"}},`"result`":{`"id`":`"create_labor:brass_press_seat`",`"count`":1}}"
WriteRecipe "brass_press_seat_from_wool" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"ATA`",`"AYA`",`"AMA`"],`"key`":{`"T`":{`"item`":`"minecraft:iron_ingot`"},`"A`":{`"item`":`"create:brass_ingot`"},`"Y`":{`"tag`":`"minecraft:wool`"},`"M`":{`"tag`":`"minecraft:wooden_slabs`"}},`"result`":{`"id`":`"create_labor:brass_press_seat`",`"count`":1}}"
WriteRecipe "brass_press_seat_mixing" "{`"type`":`"create:mixing`",`"ingredients`":[{`"tag`":`"create:seats`"},{`"item`":`"minecraft:iron_ingot`"},{`"item`":`"create:brass_ingot`"}],`"results`":[{`"id`":`"create_labor:brass_press_seat`",`"count`":1}]}"

# --- Mixer ---
WriteRecipe "brass_mixer_seat" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"BHB`",`"AZA`"],`"key`":{`"B`":{`"tag`":`"c:plates/iron`"},`"H`":{`"item`":`"create:brass_ingot`"},`"A`":{`"item`":`"create:brass_ingot`"},`"Z`":{`"tag`":`"create:seats`"}},`"result`":{`"id`":`"create_labor:brass_mixer_seat`",`"count`":1}}"

# --- Millstone ---
WriteRecipe "brass_millstone_seat_from_create_seat" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"ATA`",`"AZA`"],`"key`":{`"T`":{`"item`":`"create:cogwheel`"},`"A`":{`"item`":`"create:brass_ingot`"},`"Z`":{`"tag`":`"create:seats`"}},`"result`":{`"id`":`"create_labor:brass_millstone_seat`",`"count`":1}}"
WriteRecipe "brass_millstone_seat_from_wool" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"ATA`",`"AYA`",`"AMA`"],`"key`":{`"T`":{`"item`":`"create:cogwheel`"},`"A`":{`"item`":`"create:brass_ingot`"},`"Y`":{`"tag`":`"minecraft:wool`"},`"M`":{`"tag`":`"c:stones`"}},`"result`":{`"id`":`"create_labor:brass_millstone_seat`",`"count`":1}}"
WriteRecipe "brass_millstone_seat_mixing" "{`"type`":`"create:mixing`",`"ingredients`":[{`"tag`":`"create:seats`"},{`"item`":`"create:cogwheel`"},{`"item`":`"create:brass_ingot`"}],`"results`":[{`"id`":`"create_labor:brass_millstone_seat`",`"count`":1}]}"

# --- Deployer ---
WriteRecipe "brass_deployer_seat_from_create_seat" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"ATA`",`"AZA`"],`"key`":{`"T`":{`"item`":`"create:brass_hand`"},`"A`":{`"item`":`"create:brass_ingot`"},`"Z`":{`"tag`":`"create:seats`"}},`"result`":{`"id`":`"create_labor:brass_deployer_seat`",`"count`":1}}"
WriteRecipe "brass_deployer_seat_from_wool" "{`"type`":`"minecraft:crafting_shaped`",`"pattern`":[`"ATA`",`"AYA`",`"AMA`"],`"key`":{`"T`":{`"item`":`"create:brass_hand`"},`"A`":{`"item`":`"create:brass_ingot`"},`"Y`":{`"tag`":`"minecraft:wool`"},`"M`":{`"tag`":`"minecraft:wooden_slabs`"}},`"result`":{`"id`":`"create_labor:brass_deployer_seat`",`"count`":1}}"
WriteRecipe "brass_deployer_seat_mixing" "{`"type`":`"create:mixing`",`"ingredients`":[{`"tag`":`"create:seats`"},{`"item`":`"create:brass_hand`"},{`"item`":`"create:brass_ingot`"}],`"results`":[{`"id`":`"create_labor:brass_deployer_seat`",`"count`":1}]}"

Write-Host "Generated 13 brass direct recipes"
Write-Host "Total: 36 recipe files generated"
Write-Host "Done!"
