package ru.mirea.soshnin.nobel.data.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import ru.mirea.soshnin.nobel.domain.model.Laureate
import ru.mirea.soshnin.nobel.domain.model.NobelPrize
import ru.mirea.soshnin.nobel.domain.model.User
import ru.mirea.soshnin.nobel.domain.repository.FavoriteRepository
import ru.mirea.soshnin.nobel.domain.repository.PrizeRepository
import ru.mirea.soshnin.nobel.domain.repository.UserRepository

class PostgresPrizeRepository : PrizeRepository {

    override suspend fun getAll(year: Int?, category: String?) = dbQuery {
        val rows = Prizes.selectAll().where {
            var cond: Op<Boolean> = Op.TRUE
            if (year != null) cond = cond and (Prizes.awardYear eq year)
            if (category != null) cond = cond and (Prizes.category eq category)
            cond
        }.orderBy(Prizes.awardYear to SortOrder.DESC, Prizes.category to SortOrder.ASC)
            .toList()
        withLaureates(rows)
    }

    override suspend fun get(year: Int, category: String) = dbQuery {
        val rows = Prizes.selectAll()
            .where { (Prizes.awardYear eq year) and (Prizes.category eq category) }
            .toList()
        withLaureates(rows).firstOrNull()
    }

    override suspend fun getById(id: Int) = dbQuery {
        withLaureates(Prizes.selectAll().where { Prizes.id eq id }.toList()).firstOrNull()
    }

    // лауреаты всех найденных премий одним запросом
    private fun withLaureates(rows: List<ResultRow>): List<NobelPrize> {
        if (rows.isEmpty()) return emptyList()
        val ids = rows.map { it[Prizes.id] }
        val laureates = Laureates.selectAll()
            .where { Laureates.prizeId inList ids }
            .groupBy({ it[Laureates.prizeId] }) {
                Laureate(
                    it[Laureates.laureateId], it[Laureates.fullName],
                    it[Laureates.portion], it[Laureates.motivation],
                    it[Laureates.portraitUrl]
                )
            }
        return rows.map {
            NobelPrize(
                id = it[Prizes.id],
                awardYear = it[Prizes.awardYear],
                category = it[Prizes.category],
                categoryName = it[Prizes.categoryName],
                fullName = it[Prizes.fullName],
                dateAwarded = it[Prizes.dateAwarded],
                prizeAmount = it[Prizes.prizeAmount],
                laureates = laureates[it[Prizes.id]].orEmpty()
            )
        }
    }

    suspend fun count() = dbQuery { Prizes.selectAll().count() }

    suspend fun saveAll(prizes: List<NobelPrize>) = dbQuery {
        for (p in prizes) {
            val id = Prizes.insert {
                it[awardYear] = p.awardYear
                it[category] = p.category
                it[categoryName] = p.categoryName
                it[fullName] = p.fullName
                it[motivation] = p.laureates.firstOrNull()?.motivation.orEmpty()
                it[detailLink] = "https://api.nobelprize.org/2/nobelPrize/${p.category}/${p.awardYear}"
                it[dateAwarded] = p.dateAwarded
                it[prizeAmount] = p.prizeAmount
            }[Prizes.id]
            Laureates.batchInsert(p.laureates) { l ->
                this[Laureates.prizeId] = id
                this[Laureates.laureateId] = l.id
                this[Laureates.fullName] = l.fullName
                this[Laureates.portion] = l.portion
                this[Laureates.motivation] = l.motivation
                this[Laureates.portraitUrl] = l.portraitUrl
            }
        }
    }
}

class PostgresUserRepository : UserRepository {
    private fun ResultRow.toUser() =
        User(this[Users.id], this[Users.username], this[Users.passwordHash], this[Users.role])

    override suspend fun findByUsername(username: String) = dbQuery {
        Users.selectAll().where { Users.username eq username }.firstOrNull()?.toUser()
    }

    override suspend fun findById(id: Int) = dbQuery {
        Users.selectAll().where { Users.id eq id }.firstOrNull()?.toUser()
    }

    suspend fun createIfMissing(username: String, hash: String, role: String) = dbQuery {
        if (Users.selectAll().where { Users.username eq username }.empty()) {
            Users.insert {
                it[Users.username] = username
                it[passwordHash] = hash
                it[Users.role] = role
            }
        }
    }
}

class PostgresFavoriteRepository(
    private val prizes: PostgresPrizeRepository
) : FavoriteRepository {

    override suspend fun list(userId: Int): List<NobelPrize> {
        val ids = dbQuery {
            UserPrizes.selectAll().where { UserPrizes.userId eq userId }
                .orderBy(UserPrizes.addedAt to SortOrder.DESC)
                .map { it[UserPrizes.prizeId] }
        }
        return ids.mapNotNull { prizes.getById(it) }
    }

    override suspend fun add(userId: Int, prizeId: Int) = dbQuery {
        UserPrizes.insertIgnore {
            it[UserPrizes.userId] = userId
            it[UserPrizes.prizeId] = prizeId
        }.insertedCount > 0
    }

    override suspend fun remove(userId: Int, prizeId: Int) = dbQuery {
        UserPrizes.deleteWhere {
            (UserPrizes.userId eq userId) and (UserPrizes.prizeId eq prizeId)
        } > 0
    }
}
