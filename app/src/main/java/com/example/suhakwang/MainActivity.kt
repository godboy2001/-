package com.example.suhakwang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

data class UnitInfo(val term: String, val name: String) {
    val title: String get() = "$term · $name"
}
data class Question(val id: String, val text: String, val answer: Int, val explanation: String)

private val units = listOf(
    UnitInfo("1학기", "덧셈과 뺄셈"),
    UnitInfo("1학기", "평면도형"),
    UnitInfo("1학기", "나눗셈"),
    UnitInfo("1학기", "곱셈"),
    UnitInfo("1학기", "길이와 시간"),
    UnitInfo("1학기", "분수와 소수"),
    UnitInfo("2학기", "곱셈"),
    UnitInfo("2학기", "나눗셈"),
    UnitInfo("2학기", "원"),
    UnitInfo("2학기", "분수"),
    UnitInfo("2학기", "들이와 무게"),
    UnitInfo("2학기", "자료의 정리")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF2859A8))) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MathApp(
                        loadWrong = { loadWrong() },
                        saveWrong = { saveWrong(it) },
                        loadStats = { loadStats() },
                        saveStats = { saveStats(it) }
                    )
                }
            }
        }
    }

    private fun loadWrong(): MutableMap<String, MutableList<Question>> {
        val raw = getPreferences(MODE_PRIVATE).getString("wrong", "{}") ?: "{}"
        val obj = JSONObject(raw)
        val result = mutableMapOf<String, MutableList<Question>>()
        obj.keys().forEach { key ->
            val arr = obj.getJSONArray(key)
            result[key] = MutableList(arr.length()) { i ->
                val q = arr.getJSONObject(i)
                Question(q.getString("id"), q.getString("text"), q.getInt("answer"), q.getString("explanation"))
            }
        }
        return result
    }

    private fun saveWrong(data: Map<String, List<Question>>) {
        val obj = JSONObject()
        data.forEach { (key, list) ->
            val arr = JSONArray()
            list.forEach { q ->
                arr.put(JSONObject().put("id", q.id).put("text", q.text).put("answer", q.answer).put("explanation", q.explanation))
            }
            obj.put(key, arr)
        }
        getPreferences(MODE_PRIVATE).edit().putString("wrong", obj.toString()).apply()
    }

    private fun loadStats(): Map<String, Int> {
        val obj = JSONObject(getPreferences(MODE_PRIVATE).getString("stats", "{}") ?: "{}")
        return obj.keys().asSequence().associateWith { obj.optInt(it, 0) }
    }

    private fun saveStats(data: Map<String, Int>) {
        val obj = JSONObject()
        data.forEach { (k, v) -> obj.put(k, v) }
        getPreferences(MODE_PRIVATE).edit().putString("stats", obj.toString()).apply()
    }
}

@Composable
private fun MathApp(
    loadWrong: () -> MutableMap<String, MutableList<Question>>,
    saveWrong: (Map<String, List<Question>>) -> Unit,
    loadStats: () -> Map<String, Int>,
    saveStats: (Map<String, Int>) -> Unit
) {
    var selectedUnit by remember { mutableStateOf<Int?>(null) }
    var level by remember { mutableStateOf("중") }
    var questions by remember { mutableStateOf(listOf<Question>()) }
    var index by remember { mutableIntStateOf(0) }
    var typed by remember { mutableStateOf("") }
    var selectedChoice by remember { mutableStateOf<Int?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var answered by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf("home") } // home, quiz, result, wrong
    var wrongMap by remember { mutableStateOf(loadWrong()) }
    var stats by remember { mutableStateOf(loadStats()) }
    var retryQuestions by remember { mutableStateOf(false) }

    fun startQuiz(unit: Int, retry: Boolean = false) {
        selectedUnit = unit
        retryQuestions = retry
        val key = "${units[unit].title}-$level"
        questions = if (retry) {
            wrongMap[key]?.toList() ?: emptyList()
        } else {
            (1..20).map { makeQuestion(unit, level, it) }
        }
        index = 0; score = 0; typed = ""; selectedChoice = null
        feedback = null; answered = false
        mode = if (questions.isEmpty()) "emptyWrong" else "quiz"
    }

    fun submitAnswer(value: Int?) {
        if (answered || value == null || selectedUnit == null) return
        val q = questions[index]
        val correct = value == q.answer
        val key = "${units[selectedUnit!!].title}-$level"
        val list = wrongMap[key] ?: mutableListOf()
        if (correct) {
            score++
            if (retryQuestions) {
                wrongMap[key] = list.filterNot { it.id == q.id }.toMutableList()
            } else {
                wrongMap[key] = list.filterNot { it.id == q.id }.toMutableList()
            }
            feedback = "정답이에요! 잘했어요 🎉"
        } else {
            if (list.none { it.id == q.id }) list.add(q)
            wrongMap[key] = list
            feedback = "아쉬워요. 정답은 ${q.answer}이에요.\n풀이: ${q.explanation}"
        }
        saveWrong(wrongMap.mapValues { it.value.toList() })
        answered = true
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🧮 수학왕 3학년", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (mode != "home") TextButton(onClick = { mode = "home" }) { Text("처음으로") }
        }
        Spacer(Modifier.height(12.dp))
        when (mode) {
            "home" -> {
                Text("오늘도 즐겁게 수학 공부!", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Text("난이도 선택", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("하", "중", "상").forEach { item ->
                        FilterChip(selected = level == item, onClick = { level = item }, label = { Text("$item 난이도") })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("학습할 단원", fontWeight = FontWeight.Bold)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(units.indices.toList()) { i ->
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(units[i].title, fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("20문제 · ${level} 난이도", style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.weight(1f))
                                    Button(onClick = { startQuiz(i) }) { Text("학습하기") }
                                }
                                val key = "${units[i].title}-$level"
                                val count = wrongMap[key]?.size ?: 0
                                Text("오답 노트: $count문제", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                TextButton(onClick = { startQuiz(i, true) }, enabled = count > 0) { Text("틀린 문제 다시 풀기") }
                            }
                        }
                    }
                }
            }
            "quiz" -> {
                val q = questions[index]
                LinearProgressIndicator(progress = { (index + 1) / questions.size.toFloat() }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("${units[selectedUnit!!].title}  ·  $level 난이도", style = MaterialTheme.typography.titleSmall)
                Text("${index + 1} / ${questions.size}번 문제", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(18.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text(q.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp))
                        OutlinedTextField(
                            value = typed, onValueChange = { typed = it; selectedChoice = null },
                            label = { Text("정답을 숫자로 입력하세요") }, modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !answered
                        )
                        Spacer(Modifier.height(8.dp))
                        if (!answered) Button(
                            onClick = { submitAnswer(typed.toIntOrNull()) },
                            enabled = typed.toIntOrNull() != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("정답 확인") }
                        feedback?.let { Text(it, modifier = Modifier.padding(top = 12.dp), color = if (it.startsWith("정답")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                        if (answered) {
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                if (index < questions.lastIndex) {
                                    index++; typed = ""; feedback = null; answered = false
                                } else {
                                    val key = "${units[selectedUnit!!].title}-$level"
                                    stats = stats.toMutableMap().also { it[key] = score }
                                    saveStats(stats)
                                    mode = "result"
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text(if (index < questions.lastIndex) "다음 문제" else "결과 보기") }
                        }
                    }
                }
            }
            "result" -> {
                Text("학습 완료! 🎉", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${questions.size}문제 중 $score문제를 맞혔어요.")
                Text("정답률: ${if (questions.isEmpty()) 0 else score * 100 / questions.size}%")
                Spacer(Modifier.height(12.dp))
                val key = "${units[selectedUnit!!].title}-$level"
                val count = wrongMap[key]?.size ?: 0
                Text("오답 노트에 $count문제가 있어요.")
                Button(onClick = { startQuiz(selectedUnit!!, true) }, enabled = count > 0, modifier = Modifier.fillMaxWidth()) { Text("틀린 문제 다시 풀기") }
                OutlinedButton(onClick = { mode = "home" }, modifier = Modifier.fillMaxWidth()) { Text("단원 선택으로") }
            }
            "emptyWrong" -> {
                Text("오답 노트가 비어 있어요 😊", style = MaterialTheme.typography.titleLarge)
                Text("문제를 풀고 틀린 문제가 생기면 여기서 다시 도전할 수 있어요.")
                Button(onClick = { mode = "home" }) { Text("돌아가기") }
            }
        }
    }
}

private fun makeQuestion(unit: Int, level: String, n: Int): Question {
    val r = Random(unit * 10000 + n * 73 + level.hashCode())
    val scale = when (level) { "하" -> 10; "중" -> 100; else -> 500 }
    val a = r.nextInt(2, scale + 1)
    val b = r.nextInt(2, scale + 1)
    val (text, answer, explain) = when (unit) {
        0 -> Triple("$a + $b 는 얼마일까요?", a + b, "$a와 $b를 더하면 ${a+b}입니다.")
        1 -> {
            val sides = if (n % 2 == 0) 4 else 3
            Triple("변이 ${sides}개인 도형의 이름은 무엇일까요? (삼각형=3, 사각형=4)", sides, "변이 $sides개인 도형은 ${if(sides==3) "삼각형" else "사각형"}입니다. 정답 입력은 변의 개수($sides)입니다.")
        }
        2, 7 -> {
            val divisor = r.nextInt(2, 10)
            val quotient = r.nextInt(2, if (level == "상") 30 else 15)
            val total = divisor * quotient
            Triple("$total ÷ $divisor 는 얼마일까요?", quotient, "$total개를 $divisor개씩 나누면 ${quotient}묶음입니다.")
        }
        3, 6 -> {
            val x = r.nextInt(2, if (level == "상") 30 else 12)
            val y = r.nextInt(2, if (level == "상") 20 else 10)
            Triple("$x × $y 는 얼마일까요?", x*y, "$x를 $y번 더하면 ${x*y}입니다.")
        }
        4 -> {
            val cm = r.nextInt(1, if (level == "상") 20 else 10)
            Triple("${cm}cm는 몇 mm일까요?", cm*10, "1cm는 10mm이므로 ${cm}×10=${cm*10}mm입니다.")
        }
        5, 9 -> {
            val den = listOf(2, 3, 4, 5, 8, 10)[r.nextInt(6)]
            val num = r.nextInt(1, den)
            Triple("전체를 똑같이 $den조각으로 나눈 것 중 $num조각은 전체의 얼마일까요? (분자만 입력)", num, "분수는 ${num}/$den이고 분자는 $num입니다.")
        }
        8 -> {
            val radius = r.nextInt(1, 10)
            Triple("반지름이 ${radius}cm인 원의 지름은 몇 cm일까요?", radius*2, "지름은 반지름의 2배이므로 ${radius}×2=${radius*2}cm입니다.")
        }
        10 -> {
            val liters = r.nextInt(1, 10)
            Triple("${liters}L는 몇 mL일까요?", liters*1000, "1L는 1000mL이므로 ${liters}L는 ${liters*1000}mL입니다.")
        }
        else -> {
            val x = r.nextInt(2, 10)
            val y = r.nextInt(2, 10)
            Triple("사과가 ${x}개, 배가 ${y}개 있습니다. 과일은 모두 몇 개일까요?", x+y, "$x+$y=${x+y}개입니다.")
        }
    }
    return Question("u${unit}_l${level}_q$n", text, answer, explain)
}
