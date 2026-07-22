# Generate material-variant assets for CreateLabor Villager Forge 1.20.1
$baseDir = "e:\git\Minecraft\MC MOD\CL CreateLabor Villager\1.20.1-Forge\src\main\resources\assets\create_labor"
$texDir = "$baseDir\textures\block"
$modelDir = "$baseDir\models\block"
$bsDir = "$baseDir\blockstates"
$itemDir = "$baseDir\models\item"

# 1. Copy textures: seat/ -> andesite/, copper/, brass/
$srcSeat = "$texDir\seat"
foreach ($mat in @("andesite", "copper", "brass")) {
    $dest = "$texDir\$mat"
    if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
    Copy-Item $srcSeat $dest -Recurse
    Write-Host "Copied textures to $mat"
}

# 2. Delete old model files (all seat-related)
Get-ChildItem $modelDir -Filter "*_seat*.json" | Remove-Item -Force
Write-Host "Deleted old block models"

# 3. Delete old blockstate files
Get-ChildItem $bsDir -Filter "*_seat.json" | Remove-Item -Force
Write-Host "Deleted old blockstates"

# 4. Delete old item model files (keep villager_binder.json)
Get-ChildItem $itemDir -Filter "*_seat.json" | Remove-Item -Force
Write-Host "Deleted old item models"

# Data definitions
$colors = @("white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black")
$types = @(
    @{name="press"; prefix="pre"},
    @{name="mixer"; prefix="mix"},
    @{name="saw"; prefix="saw"},
    @{name="millstone"; prefix="mil"},
    @{name="deployer"; prefix="dep"}
)
$materials = @("andesite", "copper", "brass")

$displayStandard = '"thirdperson_righthand": { "rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375] },
        "thirdperson_lefthand": { "rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375] },
        "firstperson_righthand": { "rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4] },
        "firstperson_lefthand": { "rotation": [0, 225, 0], "scale": [0.4, 0.4, 0.4] },
        "ground": { "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25] },
        "gui": { "rotation": [30, 225, 0], "translation": [0, 2, 0], "scale": [0.625, 0.625, 0.625] },
        "fixed": { "scale": [0.5, 0.5, 0.5] }'

$displayDeployer = '"thirdperson_righthand": { "rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375] },
        "thirdperson_lefthand": { "rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375] },
        "firstperson_righthand": { "rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4] },
        "firstperson_lefthand": { "rotation": [0, -135, 0], "scale": [0.4, 0.4, 0.4] },
        "ground": { "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25] },
        "gui": { "rotation": [30, -135, 0], "translation": [0, 2, 0], "scale": [0.625, 0.625, 0.625] },
        "fixed": { "scale": [0.5, 0.5, 0.5] }'

$count = 0
foreach ($mat in $materials) {
    foreach ($type in $types) {
        $typeName = $type.name
        $typePrefix = $type.prefix
        $display = if ($typeName -eq "deployer") { $displayDeployer } else { $displayStandard }

        # 5. Generate base model
        $baseJson = "{`n" +
        "    `"credit`": `"Made with Blockbench`",`n" +
        "    `"parent`": `"block/block`",`n" +
        "    `"textures`": {`n" +
        "        `"bottom`": `"create_labor:block/$mat/bottom`",`n" +
        "        `"top`": `"create_labor:block/$mat/top_white`",`n" +
        "        `"side`": `"create_labor:block/$mat/${typeName}_side`",`n" +
        "        `"particle`": `"#top`"`n" +
        "    },`n" +
        "    `"elements`": [`n" +
        "        {`n" +
        "            `"from`": [0, 0, 0],`n" +
        "            `"to`": [16, 8, 16],`n" +
        "            `"faces`": {`n" +
        "                `"north`": {`"uv`": [0, 8, 16, 16], `"texture`": `"#side`"},`n" +
        "                `"east`": {`"uv`": [0, 8, 16, 16], `"texture`": `"#side`"},`n" +
        "                `"south`": {`"uv`": [0, 8, 16, 16], `"texture`": `"#side`"},`n" +
        "                `"west`": {`"uv`": [0, 8, 16, 16], `"texture`": `"#side`"},`n" +
        "                `"up`": {`"uv`": [0, 0, 16, 16], `"texture`": `"#top`"},`n" +
        "                `"down`": {`"uv`": [0, 0, 16, 16], `"texture`": `"#bottom`"}`n" +
        "            }`n" +
        "        }`n" +
        "    ],`n" +
        "    `"display`": {`n" +
        "        $display`n" +
        "    }`n" +
        "}"
        $baseFile = "$modelDir\${mat}_${typeName}_seat_base.json"
        [System.IO.File]::WriteAllText($baseFile, $baseJson)
        $count++

        # 6. Generate color variant models (off and on, 16 colors each)
        foreach ($state in @("off", "on")) {
            foreach ($color in $colors) {
                $variantJson = "{`n" +
                "    `"parent`": `"create_labor:block/${mat}_${typeName}_seat_base`",`n" +
                "    `"textures`": {`n" +
                "        `"top`": `"create_labor:block/$mat/top_$color`",`n" +
                "        `"side`": `"create_labor:block/$mat/side_${typePrefix}_$state`"`n" +
                "    }`n" +
                "}"
                $variantFile = "$modelDir\${mat}_${typeName}_seat_${state}_${color}.json"
                [System.IO.File]::WriteAllText($variantFile, $variantJson)
                $count++
            }
        }

        # 7. Generate blockstate (32 variants: 16 colors x 2 working states)
        $variantLines = @()
        foreach ($color in $colors) {
            $variantLines += "        `"color=$color,working=false`": { `"model`": `"create_labor:block/${mat}_${typeName}_seat_off_$color`" }"
            $variantLines += "        `"color=$color,working=true`": { `"model`": `"create_labor:block/${mat}_${typeName}_seat_on_$color`" }"
        }
        $bsJson = "{`n    `"variants`": {`n" + ($variantLines -join ",`n") + "`n    }`n}"
        $bsFile = "$bsDir\${mat}_${typeName}_seat.json"
        [System.IO.File]::WriteAllText($bsFile, $bsJson)
        $count++

        # 8. Generate item model
        $itemJson = "{`n    `"parent`": `"create_labor:block/${mat}_${typeName}_seat_off_light_blue`"`n}"
        $itemFile = "$itemDir\${mat}_${typeName}_seat.json"
        [System.IO.File]::WriteAllText($itemFile, $itemJson)
        $count++
    }
}

# 9. Delete old seat/ texture folder (textures now in material-specific folders)
Remove-Item $srcSeat -Recurse -Force
Write-Host "Deleted old seat/ texture folder"

Write-Host "Generated $count files total"
Write-Host "Done!"
