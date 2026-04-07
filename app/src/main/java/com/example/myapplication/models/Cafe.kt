package com.example.pathfinding.models

data class Cafe(
    val id: String,
    val name: String,
    val x: Double,
    val y: Double,
    val openHour: Int,
    val closeHour: Int,
    val menu: List<String>
)

object DataProvider {
    val cafes = listOf(
        Cafe("sib_blini", "Сибирские блины", 1.0, 2.0, 9, 21, listOf("блины", "чай")),
        Cafe("starbooks", "Starbooks", 3.0, 5.0, 8, 22, listOf("кофе", "капучино", "чай")),
        Cafe("main_cafeteria", "Главная столовая", 5.0, 5.0, 10, 18, listOf("полноценный обед", "суп", "салат")),
        Cafe("yarche", "Ярче", 7.0, 8.0, 9, 21, listOf("снэки", "одноразовая посуда", "напитки")),
        Cafe("bus_stop_coffee", "Кофе у остановки", 2.0, 7.0, 7, 19, listOf("кофе", "чай", "булочки")),
        Cafe("second_building_cafe", "Кафе в корпусе 2", 6.0, 2.0, 9, 17, listOf("кофе", "пирожки")),
        Cafe("vending_machine", "Вендинг", 4.0, 4.0, 0, 24, listOf("снэки", "напитки"))
    )

    val dishes = listOf(
        Dish("Блины", "sib_blini"),
        Dish("Кофе", "starbooks"),
        Dish("Полноценный обед", "main_cafeteria"),
        Dish("Снэки", "yarche"),
        Dish("Чай", "bus_stop_coffee"),
        Dish("Одноразовая посуда", "yarche"),
        Dish("Пирожки", "second_building_cafe")
    )

    val pointsOfInterest = listOf(
        PointOfInterest("main_building", "Главный корпус", 1.0, 1.0),
        PointOfInterest("library", "Научная библиотека", 3.0, 4.0),
        PointOfInterest("old_dorm", "Старое общежитие", 6.0, 2.0),
        PointOfInterest("sport_complex", "Спорткомплекс", 8.0, 7.0),
        PointOfInterest("botanical_garden", "Ботанический сад", 2.0, 9.0),
        PointOfInterest("campus_center", "Центр кампуса", 5.0, 5.0),
        PointOfInterest("pond", "Пруд", 7.0, 8.0)
    )
}

data class Dish(val name: String, val cafeId: String)