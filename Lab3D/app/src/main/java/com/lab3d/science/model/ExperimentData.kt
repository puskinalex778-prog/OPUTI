package com.lab3d.science.model

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class ExperimentId(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val subject: String,
    val accentHex: Long,
    val secondaryHex: Long,
    val formula: String,
    val explanation: String,
    val funFact: String,
) {
    VOLCANO(
        title = "3D Вулкан и Химия",
        subtitle = "Реакция соды и уксуса, газ CO₂ и пена",
        emoji = "🌋",
        subject = "Химия",
        accentHex = 0xFFFF6B35,
        secondaryHex = 0xFFFF2E63,
        formula = "NaHCO₃ + CH₃COOH → CO₂↑ + H₂O + соль",
        explanation = "Пищевая сода — это щёлочь, а уксус — кислота. Когда они встречаются, начинается бурная реакция нейтрализации и мгновенно выделяется углекислый газ (CO₂). Мыло задерживает пузырьки газа и превращает их в гигантский поток густой «лавы»!",
        funFact = "Настоящие вулканы тоже извергаются из-за давления газов, растворённых в раскалённой магме глубоко под землёй!",
    ),
    CIRCUIT(
        title = "Электрическая цепь",
        subtitle = "Закон Ома, электроны, мотор и электромагнит",
        emoji = "⚡",
        subject = "Физика",
        accentHex = 0xFFFFD84F,
        secondaryHex = 0xFFFF8C24,
        formula = "I = U / R   ·   P = U × I",
        explanation = "Напряжение батарейки (U, Вольты) толкает электроны по проводам. Сопротивление (R, Омы) мешает им бежать слишком быстро. Чем выше напряжение и ниже сопротивление, тем сильнее ток (I, Амперы) — лампочка светит ярче, мотор крутится быстрее, а электромагнит поднимает железо!",
        funFact = "Если ток станет слишком большим (больше 4 А), нить лампочки перегреется и сгорит с искрами — поэтому в домах ставят предохранители!",
    ),
    OPTICS(
        title = "Призма Ньютона и Лазер",
        subtitle = "Преломление света, радуга и алмаз",
        emoji = "🌈",
        subject = "Оптика",
        accentHex = 0xFF38F2FF,
        secondaryHex = 0xFF7B4DFF,
        formula = "n₁ · sin(α) = n₂ · sin(β)",
        explanation = "Белый свет состоит из всех цветов радуги! Когда луч входит в стекло или алмаз, он замедляется и преломляется (меняет направление). Фиолетовые лучи имеют более короткую волну и преломляются сильнее, чем красные — так призма раскладывает свет в настоящую радугу!",
        funFact = "Алмаз имеет огромный показатель преломления (n = 2.42) — лучи внутри него многократно отражаются, поэтому бриллианты так ярко сверкают!",
    ),
    MAGNET(
        title = "Магнитное поле и Левитация",
        subtitle = "Полюса N/S, компасы и парение в воздухе",
        emoji = "🧲",
        subject = "Магнетизм",
        accentHex = 0xFFFF4D8D,
        secondaryHex = 0xFF3B82F6,
        formula = "N ↔ S притяжение  ·  N ⟷ N отталкивание",
        explanation = "Вокруг каждого магнита есть невидимое 3D-поле, выходящее из Северного полюса (N) и входящее в Южный (S). Разные полюса притягиваются, а одинаковые — отталкиваются. Если направить одинаковые полюса друг на друга вертикально, верхний магнит повиснет в воздухе!",
        funFact = "Скоростные поезда Маглев (магнитная левитация) буквально парят над рельсами на мощных магнитах и разгоняются до 600 км/ч!",
    ),
    ROCKET(
        title = "Запуск 3D Ракеты",
        subtitle = "Реактивная тяга, планеты и выход в космос",
        emoji = "🚀",
        subject = "Космонавтика",
        accentHex = 0xFF4ADE80,
        secondaryHex = 0xFF06B6D4,
        formula = "a = (F_тяги − F_сопр) / m − g",
        explanation = "По третьему закону Ньютона ракета с огромной силой выбрасывает горячий газ вниз, а газ толкает ракету вверх! Чем больше топлива, тем тяжелее ракета на старте, но по мере сгорания она становится легче и ускоряется всё быстрее. А на Луне или Марсе гравитация слабее, поэтому взлететь проще!",
        funFact = "Линия Кармана — официальная граница космоса — проходит на высоте 100 км над поверхностью Земли.",
    ),
    GRAVITY(
        title = "Гравитация и Орбиты",
        subtitle = "Запусти планету или комету вокруг Солнца",
        emoji = "🪐",
        subject = "Астрономия",
        accentHex = 0xFFA855F7,
        secondaryHex = 0xFFEC4899,
        formula = "F = G · (M · m) / r²",
        explanation = "Солнце притягивает планеты своей огромной массой. Почему планеты не падают на Солнце? Потому что они летят вбок с огромной скоростью и как бы «промахиваются» мимо него! Если скорость слишком мала — планета упадёт на Солнце, если слишком велика — улетит в глубокий космос!",
        funFact = "Земля несётся по своей орбите вокруг Солнца со скоростью почти 30 километров в секунду!",
    );

    val accentColor: Color get() = Color(accentHex)
    val secondaryColor: Color get() = Color(secondaryHex)
}

data class Mission(
    val id: String,
    val experiment: ExperimentId,
    val title: String,
    val hint: String,
    val xp: Int = 50,
)

data class QuizQuestion(
    val experiment: ExperimentId,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val feedback: String,
)

object LabCatalog {
    val missions: List<Mission> = listOf(
        // Вулкан
        Mission("vol_1", ExperimentId.VOLCANO, "Первое извержение", "Добавь соду и уксус и нажми «Запустить реакцию!»"),
        Mission("vol_2", ExperimentId.VOLCANO, "Мега-пена (> 90 см)", "Добавь мыло, побольше соды и уксуса, чтобы пена взлетела выше 90 см!"),
        Mission("vol_3", ExperimentId.VOLCANO, "Инопланетная лава", "Выбери зелёный или фиолетовый краситель и запусти извержение!"),

        // Электричество
        Mission("cir_1", ExperimentId.CIRCUIT, "Зажги лампочку", "Замкни рубильник и подбери ток от 1.0 до 3.5 А без перегорания!"),
        Mission("cir_2", ExperimentId.CIRCUIT, "Турбо-пропеллер", "Переключи нагрузку на Мотор и раскрути пропеллер током выше 2.0 А!"),
        Mission("cir_3", ExperimentId.CIRCUIT, "Сила электромагнита", "Выбери Электромагнит и подними железные гайки со стола (ток > 1.8 А)!"),

        // Оптика
        Mission("opt_1", ExperimentId.OPTICS, "Радуга Ньютона", "Включи Белый свет и поверни призму так, чтобы на экране зажглась яркая радуга!"),
        Mission("opt_2", ExperimentId.OPTICS, "Сила алмаза", "Переключи материал призмы на Алмаз (n = 2.42) при белом свете!"),
        Mission("opt_3", ExperimentId.OPTICS, "Фиолетовый лазер", "Переключись на Моно-лазер и выбери короткую волну (< 450 нм) — заметь сильный изгиб луча!"),

        // Магнетизм
        Mission("mag_1", ExperimentId.MAGNET, "Магнитная левитация", "Включи режим «Левитация» и заставь верхний магнит парить в воздухе!"),
        Mission("mag_2", ExperimentId.MAGNET, "Сила неодима", "Выбери магнит «Неодим N52» в режиме левитации и подними диск выше!"),
        Mission("mag_3", ExperimentId.MAGNET, "Притяжение полюсов", "Переключись на режим «Поле 3D» и сблизь разноимённые полюса N и S!"),

        // Ракета
        Mission("roc_1", ExperimentId.ROCKET, "Полёт с Луны", "Выбери Луну (g = 1.6) и выведи ракету выше 100 км в космос!"),
        Mission("roc_2", ExperimentId.ROCKET, "Покорение Земли", "Выбери Землю (g = 9.8), настрой тягу и топливо и преодолей 100 км!"),
        Mission("roc_3", ExperimentId.ROCKET, "Старт с Марса", "Запусти ракету с Марса (g = 3.7) и достигни космоса (> 100 км)!"),

        // Гравитация
        Mission("grv_1", ExperimentId.GRAVITY, "Запуск планеты", "Запусти новую планету и удержи её на стабильной орбите (минимум 1 оборот)!"),
        Mission("grv_2", ExperimentId.GRAVITY, "Космический побег", "Запусти тело с большой скоростью (> 1.45×), чтобы оно вырвалось от Солнца!"),
        Mission("grv_3", ExperimentId.GRAVITY, "Супер-Солнце", "Увеличь массу Солнца выше 1.8× и посмотри, как ускорятся орбиты!"),
    )

    val quizzes: List<QuizQuestion> = listOf(
        // Вулкан
        QuizQuestion(
            ExperimentId.VOLCANO,
            "Какой газ выделяется при смешивании соды и уксуса?",
            listOf("Кислород (O₂)", "Углекислый газ (CO₂)", "Гелий (He)"),
            1,
            "Верно! Именно пузырьки углекислого газа CO₂ выталкивают пену из вулкана!",
        ),
        QuizQuestion(
            ExperimentId.VOLCANO,
            "Зачем в опыт с вулканом добавляют немного жидкого мыла?",
            listOf("Чтобы сделать лаву горячее", "Чтобы мыло удержало пузырьки газа в густой пене", "Чтобы остановить реакцию"),
            1,
            "Точно! Мыльная плёнка ловит газ CO₂ и делает извержение густым и высоким.",
        ),
        QuizQuestion(
            ExperimentId.VOLCANO,
            "К какому типу веществ относится уксус?",
            listOf("Кислота", "Щёлочь", "Металл"),
            0,
            "Молодец! Уксус — это слабая органическая кислота, а сода — щёлочь.",
        ),

        // Электричество
        QuizQuestion(
            ExperimentId.CIRCUIT,
            "Как изменится сила тока, если увеличить напряжение батарейки вдвое?",
            listOf("Уменьшится вдвое", "Не изменится", "Увеличится вдвое"),
            2,
            "Правильно! По закону Ома I = U / R ток прямо пропорционален напряжению.",
        ),
        QuizQuestion(
            ExperimentId.CIRCUIT,
            "Что делает резистор в электрической цепи?",
            listOf("Вырабатывает электричество", "Ограничивает (тормозит) силу тока", "Увеличивает напряжение"),
            1,
            "Верно! Резистор создаёт сопротивление (Омы) и защищает приборы от слишком сильного тока.",
        ),
        QuizQuestion(
            ExperimentId.CIRCUIT,
            "Почему катушка с током притягивает железные гайки?",
            listOf("Электрический ток создаёт вокруг провода магнитное поле", "Медь всегда магнитная", "От нагрева провод становится липким"),
            0,
            "Супер! Это явление называется электромагнетизмом — на нём работают моторы и краны.",
        ),

        // Оптика
        QuizQuestion(
            ExperimentId.OPTICS,
            "Какой цвет радуги преломляется в стеклянной призме сильнее всего?",
            listOf("Красный", "Зелёный", "Фиолетовый"),
            2,
            "Верно! У фиолетового света самая короткая видимая волна, поэтому он изгибается сильнее.",
        ),
        QuizQuestion(
            ExperimentId.OPTICS,
            "Как называется разложение белого света на цвета радуги?",
            listOf("Дисперсия", "Гравитация", "Испарение"),
            0,
            "Отлично! Дисперсия — это зависимость преломления света от длины волны (цвета).",
        ),
        QuizQuestion(
            ExperimentId.OPTICS,
            "Почему алмаз преломляет свет гораздо сильнее воды?",
            listOf("Потому что алмаз холодный", "У алмаза выше показатель преломления (свет в нём сильнее замедляется)", "В алмазе есть встроенная лампочка"),
            1,
            "Точно! В алмазе свет движется более чем в 2.4 раза медленнее, чем в вакууме.",
        ),

        // Магнетизм
        QuizQuestion(
            ExperimentId.MAGNET,
            "Что произойдёт, если сблизить два Северных полюса (N и N) магнитов?",
            listOf("Они сильно притянутся", "Они будут отталкиваться", "Ничего не произойдёт"),
            1,
            "Верно! Одноимённые полюса (N–N и S–S) всегда отталкиваются — так работает левитация!",
        ),
        QuizQuestion(
            ExperimentId.MAGNET,
            "Какой из этих магнитов самый мощный?",
            listOf("Ферритовый", "Неодимовый (N52)", "Деревянный"),
            1,
            "Точно! Неодимовые магниты способны удерживать вес во много раз больше собственного.",
        ),
        QuizQuestion(
            ExperimentId.MAGNET,
            "Как ведут себя маленькие стрелки компасов вокруг магнита?",
            listOf("Выстраиваются вдоль линий магнитного поля от N к S", "Крутятся случайно", "Всегда смотрят вверх"),
            0,
            "Молодец! Стрелка компаса — это маленький магнит, который поворачивается вдоль линий поля.",
        ),

        // Ракета
        QuizQuestion(
            ExperimentId.ROCKET,
            "На какой высоте начинается официальная граница космоса (линия Кармана)?",
            listOf("10 км", "100 км", "1000 км"),
            1,
            "Правильно! 100 километров — граница, где воздух уже слишком разрежен для самолётов.",
        ),
        QuizQuestion(
            ExperimentId.ROCKET,
            "На какой планете (или спутнике) ракете проще всего взлететь?",
            listOf("Юпитер", "Земля", "Луна"),
            2,
            "Верно! На Луне притяжение примерно в 6 раз слабее земного (g ≈ 1.6 м/с²).",
        ),
        QuizQuestion(
            ExperimentId.ROCKET,
            "Почему ракета разгоняется всё быстрее к концу работы двигателя?",
            listOf("Топливо сгорает, ракета становится легче при той же тяге", "Космос притягивает её вверх", "Двигатель становится больше"),
            0,
            "Блестяще! По второму закону Ньютона a = F / m: чем меньше масса m, тем выше ускорение!",
        ),

        // Гравитация
        QuizQuestion(
            ExperimentId.GRAVITY,
            "Почему планеты не падают на Солнце?",
            listOf("Солнце их отталкивает", "У них есть боковая орбитальная скорость, и они постоянно «промахиваются» мимо Солнца", "В космосе нет гравитации"),
            1,
            "Верно! Орбита — это постоянное падение на звезду с такой скоростью вбок, что планета всё время пролетает мимо!",
        ),
        QuizQuestion(
            ExperimentId.GRAVITY,
            "Что случится с планетой, если резко увеличить массу Солнца?",
            listOf("Она улетит прочь", "Притяжение усилится, планета притянется ближе и ускорится", "Она остановится"),
            1,
            "Точно! Сила гравитации прямо пропорциональна массе звезды.",
        ),
        QuizQuestion(
            ExperimentId.GRAVITY,
            "Что произойдёт, если запустить планету слишком быстро?",
            listOf("Она улетит в глубокий космос, преодолев притяжение Солнца", "Она сразу упадёт в Солнце", "Она начнёт лететь назад"),
            0,
            "Правильно! Если скорость выше скорости убегания (второй космической), орбита разомкнётся.",
        ),
    )

    fun missionsFor(exp: ExperimentId): List<Mission> = missions.filter { it.experiment == exp }
    fun quizzesFor(exp: ExperimentId): List<QuizQuestion> = quizzes.filter { it.experiment == exp }
}

/** Сохранение прогресса юного учёного: XP, звания, выполненные задания и викторины. */
object LabProgress {
    private var prefs: SharedPreferences? = null

    var completedMissions by mutableStateOf<Set<String>>(emptySet())
        private set
    var solvedQuizzes by mutableStateOf<Set<String>>(emptySet())
        private set
    var xp by mutableIntStateOf(0)
        private set
    var soundEnabled by mutableStateOf(true)
        private set

    fun init(ctx: Context) {
        if (prefs != null) return
        val p = ctx.applicationContext.getSharedPreferences("lab3d_progress", Context.MODE_PRIVATE)
        prefs = p
        completedMissions = p.getStringSet("missions", emptySet())?.toSet() ?: emptySet()
        solvedQuizzes = p.getStringSet("quizzes", emptySet())?.toSet() ?: emptySet()
        xp = p.getInt("xp", 0)
        soundEnabled = p.getBoolean("sound", true)
    }

    fun rankTitle(): String = when {
        xp >= 1200 -> "🏆 Нобелевский лауреат"
        xp >= 800 -> "🎓 Профессор 3D"
        xp >= 450 -> "🔬 Главный изобретатель"
        xp >= 200 -> "⚗️ Младший исследователь"
        else -> "🧪 Юный стажёр"
    }

    fun nextRankXp(): Int = when {
        xp >= 1200 -> 1500
        xp >= 800 -> 1200
        xp >= 450 -> 800
        xp >= 200 -> 450
        else -> 200
    }

    fun completeMission(id: String): Mission? {
        if (id in completedMissions) return null
        val m = LabCatalog.missions.firstOrNull { it.id == id } ?: return null
        completedMissions = completedMissions + id
        xp += m.xp
        save()
        return m
    }

    fun markQuizSolved(exp: ExperimentId, index: Int): Boolean {
        val key = "${exp.name}_$index"
        if (key in solvedQuizzes) return false
        solvedQuizzes = solvedQuizzes + key
        xp += 30
        save()
        return true
    }

    fun toggleSound() {
        soundEnabled = !soundEnabled
        save()
    }

    fun starsFor(exp: ExperimentId): Int {
        return LabCatalog.missionsFor(exp).count { it.id in completedMissions }
    }

    private fun save() {
        val p = prefs ?: return
        p.edit()
            .putStringSet("missions", completedMissions)
            .putStringSet("quizzes", solvedQuizzes)
            .putInt("xp", xp)
            .putBoolean("sound", soundEnabled)
            .apply()
    }
}
