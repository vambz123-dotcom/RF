package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.*
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

data class MacroScript(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val gameTarget: String,
    val instructions: List<String>
)

class VPhoneViewModel(application: Application) : AndroidViewModel(application) {

    private val vPhoneDao = AppDatabase.getDatabase(application).vPhoneDao()
    private val repository = VPhoneRepository(vPhoneDao)

    // Flow of all deployed Virtual Cloud Phones
    val allPhones: StateFlow<List<VPhone>> = repository.allPhones
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Log messages for active virtual phones
    private val _phoneLogs = MutableStateFlow<Map<Int, List<String>>>(emptyMap())
    val phoneLogs: StateFlow<Map<Int, List<String>>> = _phoneLogs.asStateFlow()

    // Screen navigation state
    private val _currentScreen = MutableStateFlow("dashboard")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Loading & Operation States
    private val _isDeploying = MutableStateFlow(false)
    val isDeploying: StateFlow<Boolean> = _isDeploying.asStateFlow()

    // Selected Cloud Phone Detail
    private val _selectedPhone = MutableStateFlow<VPhone?>(null)
    val selectedPhone: StateFlow<VPhone?> = _selectedPhone.asStateFlow()

    // Local Assistant & AI State
    private val _isGeminiLoading = MutableStateFlow(false)
    val isGeminiLoading: StateFlow<Boolean> = _isGeminiLoading.asStateFlow()

    private val _geminiResponse = MutableStateFlow<String?>(null)
    val geminiResponse: StateFlow<String?> = _geminiResponse.asStateFlow()

    // Keep screen awake utility state
    val localWakelockActive = MutableStateFlow(false)
    val dimScreenActive = MutableStateFlow(false)
    val batterySavingOverlay = MutableStateFlow(false)

    // Macro lists
    private val _customMacros = MutableStateFlow<List<MacroScript>>(emptyList())
    val customMacros: StateFlow<List<MacroScript>> = _customMacros.asStateFlow()

    // Initial dummy data config and simulation ticker
    init {
        // Run simulation background loops
        viewModelScope.launch(Dispatchers.IO) {
            startSimulationLoop()
        }
        
        // Add default macro scripts
        _customMacros.value = listOf(
            MacroScript(
                name = "Ragnarok Quick Re-buff",
                description = "Automates priest or tank spell refreshing every 180s.",
                gameTarget = "Ragnarok M",
                instructions = listOf("Tap Skills (1024, 760)", "Cast Blessing (1150, 680)", "Cast Increase AGI (1150, 580)", "Wait 180 seconds", "Repeat")
            ),
            MacroScript(
                name = "MLBB Lane Pusher Bot",
                description = "Keeps alignment pushing active on custom AI matching.",
                gameTarget = "Mobile Legends",
                instructions = listOf("Attack Minions (980, 800)", "Check Map Level (60, 50)", "Tap Recall (400, 820) if Health < 25%", "Wait 15 seconds", "Repeat")
            ),
            MacroScript(
                name = "Genshin Farm Router",
                description = "Coordinates simple direction tapping for repetitive ore collection.",
                gameTarget = "Genshin Impact",
                instructions = listOf("Hold Forward 3s (220, 720)", "Tap Strike (1100, 800)", "Wait 1s", "Tap Pick Up (620, 480)", "Repeat")
            )
        )
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun selectPhone(phone: VPhone?) {
        _selectedPhone.value = phone
    }

    // Rent and Deploy Virtual Cloud Phone
    fun deployVPhone(
        name: String,
        gamePackage: String,
        afkDays: Int,
        farmMode: String,
        region: String,
        fpsPreset: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isDeploying.value = true
            delay(1800) // Simulate cloud server installation

            // Generate mock IP address
            val r = Random(System.currentTimeMillis())
            val ipAddress = "10.244.${r.nextInt(10, 255)}.${r.nextInt(1, 254)}"

            val newPhone = VPhone(
                name = name.ifBlank { "vPhone-Free-${r.nextInt(100, 999)}" },
                gamePackage = gamePackage,
                afkDaysRequested = afkDays,
                farmMode = farmMode,
                region = region,
                fpsPreset = fpsPreset,
                ipAddress = ipAddress,
                status = "ONLINE"
            )

            val newId = repository.insert(newPhone).toInt()
            
            // Add initial active logs
            _phoneLogs.update { current ->
                val list = current[newId]?.toMutableList() ?: mutableListOf()
                list.add("[SYSTEM] Node initiated on server vCluster-${region.take(3).uppercase()}-Free")
                list.add("[SYSTEM] Free license allocated for $afkDays action days.")
                list.add("[SYSTEM] Connected virtual display stream. IP: $ipAddress")
                list.add("[LAUNCH] Application package $gamePackage initialized successfully.")
                current + (newId to list)
            }
            
            _isDeploying.value = false
        }
    }

    // Toggle active server state
    fun togglePhoneStatus(phone: VPhone) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedStatus = if (phone.status == "ONLINE") "PAUSED" else "ONLINE"
            val updatedPhone = phone.copy(status = updatedStatus)
            repository.update(updatedPhone)
            if (_selectedPhone.value?.id == phone.id) {
                _selectedPhone.value = updatedPhone
            }

            _phoneLogs.update { current ->
                val list = current[phone.id]?.toMutableList() ?: mutableListOf()
                val logMsg = if (updatedStatus == "ONLINE") "[SYSTEM] Cloud stream resumed operations." else "[SYSTEM] Cloud Phone set to sleep state."
                list.add(logMsg)
                current + (phone.id to list)
            }
        }
    }

    // Request extending AFK Days (Free)
    fun requestExtendDays(phone: VPhone, additionalDays: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedPhone = phone.copy(
                afkDaysRequested = phone.afkDaysRequested + additionalDays,
                status = "ONLINE" // Resume on extend
            )
            repository.update(updatedPhone)
            if (_selectedPhone.value?.id == phone.id) {
                _selectedPhone.value = updatedPhone
            }

            _phoneLogs.update { current ->
                val list = current[phone.id]?.toMutableList() ?: mutableListOf()
                list.add("[SYSTEM] Request approved! Rent duration extended by +$additionalDays Free Days.")
                list.add("[SYSTEM] System connected. Stream active.")
                current + (phone.id to list)
            }
        }
    }

    // Delete cloud instance
    fun deleteVPhone(phone: VPhone) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(phone)
            if (_selectedPhone.value?.id == phone.id) {
                _selectedPhone.value = null
            }
            _phoneLogs.update { current ->
                current - phone.id
            }
        }
    }

    // Add local macro
    fun addMacroScript(name: String, description: String, gameTarget: String, stepText: String) {
        val instructionsList = stepText.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val newMacro = MacroScript(
            name = name,
            description = description,
            gameTarget = gameTarget,
            instructions = instructionsList
        )
        _customMacros.update { current -> current + newMacro }
    }

    // Delete local macro
    fun deleteMacro(macro: MacroScript) {
        _customMacros.update { current -> current.filter { it.id != macro.id } }
    }

    // AI Assistant: Ask Gemini for optimal AFK setup/script tips
    fun askGeminiForGuide(gameAndObjective: String) {
        viewModelScope.launch {
            _isGeminiLoading.value = true
            _geminiResponse.value = null

            val finalPrompt = """
                You are V-Finger AI, the smart Gaming AFK Bot consultant.
                The user is using a free cloud phone system / mobile macro auto-tapper.
                They need help with this game or goal: "$gameAndObjective".
                
                Please provide:
                1. A brief optimization strategy (how to build level/rewards safely, what items to equip to survive auto combat).
                2. An exact coordinate list / auto tapper instructions list. E.g.:
                   - TAPPING SEQUENCE: 
                     - Step 1: (X=1024, Y=720) (Objective: Target primary buff skill)
                     - Step 2: (X=650, Y=400) (Objective: Accept daily random dungeon quests)
                3. Pro-tips to stay undetectable (prevent bans, randomizing delays, reducing screen lockouts).
                
                Keep your answer extremely structural, concise, and professional. Write in Indonesian as requested, or match the user's Indonesian context! Thank you.
            """.trimIndent()

            try {
                // Call Gemini via direct REST using Moshi serialization
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
                    // Fallback to high-quality simulated AI responses if key is not configured yet
                    delay(1500)
                    _geminiResponse.value = generateSimulatedAiResponse(gameAndObjective)
                } else {
                    val request = GenerateContentRequest(
                        contents = listOf(
                            Content(parts = listOf(Part(text = finalPrompt)))
                        )
                    )
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.service.generateContent(apiKey, request)
                    }
                    val textRes = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    _geminiResponse.value = textRes ?: "Maaf, AI asisten tidak merespon saat ini. Menggunakan simulator offline."
                }
            } catch (e: Exception) {
                _geminiResponse.value = "Koneksi offline atau terjadi masalah: ${e.localizedMessage}. Solusi gratis simulator:\n\n" + generateSimulatedAiResponse(gameAndObjective)
            } finally {
                _isGeminiLoading.value = false
            }
        }
    }

    private fun generateSimulatedAiResponse(query: String): String {
        return """
            🚀 **V-Finger AI: Rekomendasi Pintar untuk "$query"**
            
            Berikut adalah modul set-up untuk server Cloud Phone V-Finger gratis Anda:
            
            1. **Strategi AFK Terbaik**:
               - Gunakan mode "Battery Saving" di dalam pengaturan game untuk memperkecil lag dan konsumsi bandwidth streaming cloud.
               - Pasang sistem recovery bot otomatis pada instansi V-Finger (porsi HP otomatis diset ke 80% agar tidak tereliminasi).
               - Kosongkan tas inventaris setiap 12 jam (aktifkan opsi Salvage Junk di menu taktik V-Finger).
               
            2. **Rekomendasi Skrip Auto Tapper (Koordinat 1920x1080)**:
               - **Langkah 1**: Tap (X: 1150, Y: 850) - Klik tombol Serbu / Auto Combat
               - **Langkah 2**: Tap (X: 950, Y: 180) - Terima Misi Otomatis Harian
               - **Langkah 3**: Tap (X: 1860, Y: 60) - Tutup Iklan/Buku Panduan
               - **Random Delay**: Berikan waktu tunggu acak sekitar 2.4 - 3.8 detik di setiap langkah untuk mencegah deteksi bot anti-cheat!
               
            3. **Tips Keamanan Anti-Ban**:
               - Jangan AFK terus menerus selama lebih dari 24 jam berturut-turut. Pause virtual instansi vPhone Anda setiap 6 jam selama 30 menit (menggunakan fitur Scheduler V-Finger).
               - Hubungkan server V-Finger ke region lokal terdekat (Gunakan Server Singapore untuk kestabilan ping).
        """.trimIndent()
    }

    // Start background ticking simulation loop
    private suspend fun startSimulationLoop() {
        while (true) {
            delay(4000) // update stats every 4 seconds

            val currentList = allPhones.value
            if (currentList.isNotEmpty()) {
                currentList.forEach { phone ->
                    if (phone.status == "ONLINE") {
                        // Increments hours elapsed
                        val updatedHours = phone.hoursElapsed + 0.6 // simulates fast elapsed grinding
                        val isComplete = updatedHours >= (phone.afkDaysRequested * 24.0)

                        // Calculate grinding earnings increment
                        val earnedXp = phone.xpEarned + Random.nextInt(12, 45)
                        val earnedGold = phone.goldEarned + Random.nextInt(5, 25)
                        val lootChance = Random.nextFloat()
                        val earnedLoot = if (lootChance > 0.85) phone.lootCount + 1 else phone.lootCount

                        val updatedPhone = phone.copy(
                            hoursElapsed = if (isComplete) (phone.afkDaysRequested * 24.0) else updatedHours,
                            status = if (isComplete) "COMPLETED" else "ONLINE",
                            xpEarned = earnedXp,
                            goldEarned = earnedGold,
                            lootCount = earnedLoot
                        )

                        // Save updated data to room Database
                        repository.update(updatedPhone)
                        
                        // Update detail pane if currently selected
                        if (_selectedPhone.value?.id == phone.id) {
                            _selectedPhone.value = updatedPhone
                        }

                        // Appends some organic-looking mobile bot text logs!
                        _phoneLogs.update { current ->
                            val logs = current[phone.id]?.toMutableList() ?: mutableListOf()
                            
                            // Keep logs within max capacity limit (50 values)
                            if (logs.size > 50) {
                                logs.removeAt(0)
                            }

                            // Trigger log message
                            if (isComplete) {
                                logs.add("[SYSTEM] [${getCurrentTimeString()}] Rent duration elapsed! AFK of ${phone.afkDaysRequested} days complete.")
                            } else {
                                logs.add(generateMockLog(phone.gamePackage, phone.farmMode))
                            }
                            
                            current + (phone.id to logs)
                        }
                    }
                }
            }
        }
    }

    private fun getCurrentTimeString(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun generateMockLog(gamePackage: String, mode: String): String {
        val prefix = when(mode) {
            "QUESTS" -> "⚔️ [MISI] "
            "RAIDS" -> "🏰 [RAID] "
            "LUTE" -> "📦 [JARAH] "
            else -> "🎮 [COMBAT]"
        }
        
        val gameName = when(gamePackage) {
            "com.gravity.romg" -> "Ragnarok M"
            "com.lgame.roorigin" -> "Ragnarok Origin"
            "com.mobile.legends" -> "MLBB Bot"
            "com.miHoYo.GenshinImpact" -> "Genshin Farm"
            "com.pearlabyss.blackdesertm" -> "BDM Auto"
            else -> "Default Game"
        }
        
        val combatMsgs = listOf(
            "Menggiling monster liar... Menghasilkan 4,120 Critical Damage!",
            "Meluncurkan combo skill. Musuh tereliminasi!",
            "HP tersisa 72%. Bot otomatis meminum ramuan pemulih.",
            "Berjalan otomatis ke spot grinding optimal di peta.",
            "Stabilitas Cloud Phone: 100%. Frame Rate stabil di 60 FPS."
        )
        val questMsgs = listOf(
            "Melakukan auto-route ke NPC untuk menyerahkan barang bukti.",
            "Quest Harian selesai! Mendapatkan Buku XP & 2,500 Gold.",
            "Berbicara dengan Komandan Kota - Meminta misi selanjutnya.",
            "Mengambalikan misi utama ke instruktur pelatihan ksatria.",
            "Mendominasi kuis harian faksi secara mandiri."
        )
        val raidMsgs = listOf(
            "Memasuki Ruang Bawah Tanah Tingkat 4. Menunggu boss muncul.",
            "Menghindari zona serangan area merah dari Naga Kuno.",
            "Pertempuran Bos sukses! Mengklaim Hadiah Khusus Guild.",
            "Membuka peti besi dungeon dengan kunci cadangan otomatis.",
            "Menggunakan Ramuan Proteksi Jiwa sebelum duel bos berikutnya."
        )
        val luteMsgs = listOf(
            "Mendapatkan rare material: [Kristal Jiwa Ungu] x1!",
            "Menjual otomatis equip berkualitas rendah. Menguntungkan 1,820 Gold.",
            "Kapasitas tas di atas 80%: Menjalankan pembersihan sampah inventaris.",
            "Mengekstrak pecahan artefak tua untuk poin kerajinan.",
            "Harta loot disalurkan dengan aman ke dalam Cloud Storage V-Finger."
        )
        
        val messages = when(mode) {
            "QUESTS" -> questMsgs
            "RAIDS" -> raidMsgs
            "LUTE" -> luteMsgs
            else -> combatMsgs
        }
        
        return "$prefix [${getCurrentTimeString()}] $gameName: ${messages.random()}"
    }
}
