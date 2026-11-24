package ru.mirea.soshnin.nobel.data.db

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val role = varchar("role", 20).default("user")
    override val primaryKey = PrimaryKey(id)
}

object Prizes : Table("prizes") {
    val id = integer("id").autoIncrement()
    val awardYear = integer("award_year")
    val category = varchar("category", 10)
    val categoryName = varchar("category_name", 50)
    val fullName = varchar("full_name", 200)
    val motivation = text("motivation").default("")
    val detailLink = varchar("detail_link", 200).default("")
    val dateAwarded = varchar("date_awarded", 20).default("")
    val prizeAmount = integer("prize_amount").default(0)
    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(awardYear, category)
    }
}

object Laureates : Table("laureates") {
    val id = integer("id").autoIncrement()
    val prizeId = integer("prize_id").references(Prizes.id, onDelete = ReferenceOption.CASCADE)
    val laureateId = integer("laureate_id")
    val fullName = varchar("full_name", 200)
    val portion = varchar("portion", 10)
    val motivation = text("motivation")
    val portraitUrl = varchar("portrait_url", 300).nullable()
    override val primaryKey = PrimaryKey(id)
}

object UserPrizes : Table("user_prizes") {
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val prizeId = integer("prize_id").references(Prizes.id, onDelete = ReferenceOption.CASCADE)
    val addedAt = datetime("added_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(userId, prizeId)
}
