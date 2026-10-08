package com.relun.app.data.model

import java.time.Instant

/** Relationship intention. Fixed at signup; Discover and likes never cross it. */
enum class Segment(val wire: String) {
    Relationship("relationship"),
    Fun("fun");

    companion object {
        fun from(value: String?): Segment = if (value == "fun") Fun else Relationship
    }
}

data class Photo(val id: String, val url: String)

data class Person(
    val id: String,
    val name: String,
    val age: Int?,
    val photos: List<Photo>,
    val distanceKm: Double?,
    val isOnline: Boolean,
    val lastActive: Instant?,
    val bio: String,
    val occupation: String?,
    val education: String?,
    val heightCm: Double?,
    val drinking: String?,
    val smoking: String?,
    val religion: String?,
    val city: String?,
    val interests: List<String>,
    val liked: Boolean = false,
    /** Matches chat for free. */
    val isMatch: Boolean = false,
    /** Whether the user can pay to message them without a match. */
    val acceptsMessageRequests: Boolean = true,
    /** Set while the two are talking through a message request rather than a match. */
    val messageRequest: MessageRequest? = null,
) {
    val firstName: String get() = name.substringBefore(' ').ifBlank { name }
    val initial: String get() = name.firstOrNull()?.uppercase() ?: "?"
    val mainPhotoUrl: String? get() = photos.firstOrNull()?.url
}

/**
 * A paid message to someone the sender hasn't matched with. Accepted requests
 * are just matches, so only pending and declined ones are kept.
 */
data class MessageRequest(
    val declined: Boolean,
    /** True for the person who paid to send it. */
    val outgoing: Boolean,
    /** Messages the sender can still send before a reply. */
    val remaining: Int,
)

fun MessageRequestDto?.toMessageRequest(): MessageRequest? =
    if (this == null || status == "accepted") null else MessageRequest(status == "declined", outgoing, remaining)

fun PersonCardDto.toPerson(): Person = Person(
    id = user.id,
    name = user.fullName?.takeIf { it.isNotBlank() } ?: "Relun user",
    age = user.age,
    photos = photos.sortedBy { it.order }.map { Photo(it.id, it.url) },
    distanceKm = profile?.distance,
    isOnline = user.isOnline,
    lastActive = user.lastActive?.let { runCatching { Instant.parse(it) }.getOrNull() },
    bio = profile?.bio.orEmpty().trim(),
    occupation = profile?.occupation?.takeIf { it.isNotBlank() },
    education = (profile?.education ?: profile?.school)?.takeIf { it.isNotBlank() },
    heightCm = profile?.height,
    drinking = profile?.drinking?.takeIf { it.isNotBlank() },
    smoking = profile?.smoking?.takeIf { it.isNotBlank() },
    religion = profile?.religion?.takeIf { it.isNotBlank() },
    city = profile?.city?.takeIf { it.isNotBlank() },
    interests = profile?.interests.orEmpty().filter { it.isNotBlank() },
    liked = relationship?.liked ?: false,
    isMatch = relationship?.isMutual ?: false,
    acceptsMessageRequests = relationship?.acceptsMessageRequests ?: true,
    messageRequest = relationship?.messageRequest.toMessageRequest(),
)

enum class MessageStatus { Sending, Sent, Read, Failed }

data class ChatMessage(
    val id: String,
    // Set on messages this device sent, until the server echoes them back.
    val clientId: String?,
    val mine: Boolean,
    val text: String,
    val sentAt: Instant,
    val status: MessageStatus,
)

data class Conversation(
    val userId: String,
    val name: String,
    val lastMessage: String,
    val lastFromMe: Boolean,
    val lastAt: Instant,
    val unread: Int,
    val request: MessageRequest? = null,
)

data class Plus(
    /** "weekly" or "monthly". */
    val plan: String,
    /** "paystack", "paystack_pass" or "play". */
    val source: String,
    val until: Instant,
    val autoRenew: Boolean,
)

/** Free likes or message requests left. [left] is null when unlimited (Plus likes). */
data class Allowance(val limit: Int? = null, val left: Int? = null, val resetAt: Instant? = null)

data class Wallet(
    val balance: Int = 0,
    /** Likes & Views, from a coin pass or from Plus. */
    val insightsActive: Boolean = false,
    val messageRequestCost: Int = 200,
    val insights7Cost: Int = 700,
    val insights30Cost: Int = 1500,
    /** A date post beyond the free active ones. */
    val datePostCost: Int = 100,
    val packages: List<CoinPackageDto> = emptyList(),
    /** Shown once on the main screen, then marked seen. */
    val pendingBonus: PendingBonusDto? = null,
    /** Null without Plus (never had it, or it lapsed). */
    val plus: Plus? = null,
    val plans: List<PlusPlanDto> = emptyList(),
    /** What Plus includes, so copy never hard-codes it. */
    val plusPerks: PlusPerksDto = PlusPerksDto(),
    val likes: Allowance = Allowance(),
    val requests: Allowance = Allowance(limit = 1, left = 0),
    /** Free active date posts left. */
    val dates: Allowance = Allowance(limit = 1, left = 1),
)

private fun String?.toInstantOrNull(): Instant? = this?.let { runCatching { Instant.parse(it) }.getOrNull() }

fun AllowanceDto.toAllowance() = Allowance(limit, left, resetAt.toInstantOrNull())

fun PlusDto?.toPlus(): Plus? =
    if (this == null || !active) null else Plus(plan, source, until.toInstantOrNull() ?: Instant.now(), autoRenew)

enum class DateRequestStatus {
    Pending, Accepted, Declined;

    companion object {
        fun from(value: String?): DateRequestStatus? = when (value) {
            "pending" -> Pending
            "accepted" -> Accepted
            "declined" -> Declined
            else -> null
        }
    }
}

data class DatePost(
    val id: String,
    val activity: String,
    val place: String,
    val scheduledFor: Instant,
    val description: String?,
    val createdAt: Instant?,
    val owner: Person?,
    val myRequestStatus: DateRequestStatus?,
    val requests: List<DateRequest>,
)

data class DateRequest(val id: String, val person: Person, val status: DateRequestStatus)

fun DatePostDto.toDomain(): DatePost = DatePost(
    id = id,
    activity = activity,
    place = place,
    scheduledFor = runCatching { Instant.parse(scheduledFor) }.getOrDefault(Instant.now()),
    description = description?.takeIf { it.isNotBlank() },
    createdAt = createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
    owner = owner?.toPerson(),
    myRequestStatus = DateRequestStatus.from(myRequestStatus),
    requests = requests.mapNotNull { request ->
        val person = request.requester?.toPerson() ?: return@mapNotNull null
        DateRequest(request.id, person, DateRequestStatus.from(request.status) ?: DateRequestStatus.Pending)
    },
)
