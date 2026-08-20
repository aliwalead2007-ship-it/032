import re

with open('/tmp/StyleBrainSection.kt', 'r') as f:
    content = f.read()

# Replace TabRow
old_tabs = """        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardSurface,
            contentColor = GoldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = GoldPrimary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("كائنات الأنماط (${styles.size})", fontFamily = TajawalFont, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("الأنماط الماستر (${masterStyles.size})", fontFamily = TajawalFont, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }"""

new_tabs = """        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardSurface,
            contentColor = GoldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = GoldPrimary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("الأنماط", fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("دمج", fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("الماستر", fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }"""

content = content.replace(old_tabs, new_tabs)

# Replace if (selectedTab == 0) block structure with when (selectedTab)
old_selected_tab_0_start = """        if (selectedTab == 0) {"""
new_selected_tab_0_start = """        when (selectedTab) {
            0 -> {"""

content = content.replace(old_selected_tab_0_start, new_selected_tab_0_start)

old_selected_tab_1_start = """        } else {
            Text("ملفات الماستر (${masterStyles.size})", color = TextPrimary, fontFamily = CairoFont, fontSize = 18.sp, fontWeight = FontWeight.Bold)"""

new_selected_tab_1_start = """            }
            1 -> {
                Text("دمج الأساليب الممتصة", color = TextPrimary, fontFamily = CairoFont, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (styles.size < 2) {
                    Text("يجب أن يكون لديك على الأقل أسلوبان مستخرجان لتتمكن من دمجهما. قم باستخراج الأساليب من فيديوهاتك أولاً في تبويب الأنماط.", color = TextSecondary, fontFamily = TajawalFont, fontSize = 13.sp)
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("الأسلوب الأول (الأساس)", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            var expandedPrimary by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { expandedPrimary = true }, modifier = Modifier.fillMaxWidth(), border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)) {
                                    Text(primaryStyleForFusion?.name ?: "اختر الأسلوب الأول", color = TextPrimary, fontFamily = TajawalFont)
                                }
                                DropdownMenu(expanded = expandedPrimary, onDismissRequest = { expandedPrimary = false }) {
                                    styles.forEach { style ->
                                        DropdownMenuItem(
                                            text = { Text(style.name, color = TextPrimary, fontFamily = TajawalFont) },
                                            onClick = { primaryStyleForFusion = style; expandedPrimary = false }
                                        )
                                    }
                                }
                            }
                            
                            Text("الأسلوب الثاني (الداعم)", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            var expandedSecondary by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { expandedSecondary = true }, modifier = Modifier.fillMaxWidth(), border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)) {
                                    Text(secondaryStyleForFusion?.name ?: "اختر الأسلوب الثاني", color = TextPrimary, fontFamily = TajawalFont)
                                }
                                DropdownMenu(expanded = expandedSecondary, onDismissRequest = { expandedSecondary = false }) {
                                    styles.forEach { style ->
                                        DropdownMenuItem(
                                            text = { Text(style.name, color = TextPrimary, fontFamily = TajawalFont) },
                                            onClick = { secondaryStyleForFusion = style; expandedSecondary = false }
                                        )
                                    }
                                }
                            }

                            if (primaryStyleForFusion != null && secondaryStyleForFusion != null && primaryStyleForFusion?.id != secondaryStyleForFusion?.id) {
                                Text("نسبة الدمج (0.1 - 0.9)", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Slider(
                                    value = blendRatio,
                                    onValueChange = { blendRatio = it },
                                    valueRange = 0.1f..0.9f,
                                    colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary, inactiveTrackColor = Color.Gray)
                                )
                                Text("نسبة تأثير الأول: ${( (1f-blendRatio) * 100).toInt()}% | تأثير الثاني: ${(blendRatio * 100).toInt()}%", color = TextSecondary, fontFamily = TajawalFont, fontSize = 12.sp)
                                
                                OutlinedTextField(
                                    value = customFusedName,
                                    onValueChange = { customFusedName = it },
                                    label = { Text("اسم الأسلوب المدموج (اختياري)", fontFamily = TajawalFont) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldPrimary,
                                        unfocusedBorderColor = Color(0xFF1E293B),
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                StyleBrain.fuseAbsorbedStyles(
                                                    context = context,
                                                    primary = primaryStyleForFusion!!,
                                                    secondary = secondaryStyleForFusion!!,
                                                    blendRatio = blendRatio,
                                                    customName = customFusedName
                                                )
                                                message = "تم الدمج بنجاح وإنشاء أسلوب جديد!"
                                                Toast.makeText(context, "تم دمج الأساليب وحفظ الأسلوب الجديد", Toast.LENGTH_SHORT).show()
                                                selectedTab = 0 // Go back to view the new style
                                                primaryStyleForFusion = null
                                                secondaryStyleForFusion = null
                                                customFusedName = ""
                                                blendRatio = 0.5f
                                            } catch (e: Exception) {
                                                message = "فشل الدمج: ${e.message}"
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                                ) {
                                    Text("دمج الأسلوبين", color = DeepSlate, fontFamily = TajawalFont, fontWeight = FontWeight.Bold)
                                }
                            } else if (primaryStyleForFusion?.id == secondaryStyleForFusion?.id && primaryStyleForFusion != null) {
                                Text("لا يمكن دمج الأسلوب مع نفسه. يرجى اختيار أسلوب مختلف.", color = Color.Red, fontFamily = TajawalFont, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            2 -> {
                Text("ملفات الماستر (${masterStyles.size})", color = TextPrimary, fontFamily = CairoFont, fontSize = 18.sp, fontWeight = FontWeight.Bold)"""

content = content.replace(old_selected_tab_1_start, new_selected_tab_1_start)

# Add closing brace for the when statement. Wait, where does the else block end?
# Let's find the closing of the else block.
old_end = """                }
            }
        }
    }

    if (showHardResetConfirm) {"""

new_end = """                }
            }
        }
        } // End of when
    }

    if (showHardResetConfirm) {"""
content = content.replace(old_end, new_end)

with open('/tmp/StyleBrainSection2.kt', 'w') as f:
    f.write(content)

