package com.relun.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wire types for the Relun API. Field names follow the backend exactly; the
// app works with the mapped types in Models.kt instead.

// ---------- Auth ----------

@Serializable
data class ContactBody(val email: String? = null, val phone: String? = null)

@Serializable
data class VerifyOtpBody(val email: String? = null, val phone: String? = null, val otp: String)

@Serializable
data class RequestOtpResponse(val message: String? = null, val isNewUser: Boolean = false)

@Serializable
data class VerifyOtpResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUserDto,
    val needsProfileCompletion: Boolean = true,
    val photoCount: Int = 0,
)

@Serializable
data class AuthUserDto(
    val id: String,
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val coins: Int = 0,
    val profile: ProfileDto? = null,
)

@Serializable
data class RefreshBody(val refreshToken: String)

@Serializable
data class TokenPair(val accessToken: String, val refreshToken: String)

// ---------- People ----------

@Serializable
data class PhotoDto(
    @SerialName("_id") val id: String,
    val url: String,
    val order: Int = 0,
)

@Serializable
data class UserDto(
    @SerialName("_id") val id: String,
    val fullName: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val lastActive: String? = null,
    val age: Int? = null,
    val isOnline: Boolean = false,
)

@Serializable
data class ProfileDto(
    val bio: String? = null,
    val segment: String? = null,
    val occupation: String? = null,
    val education: String? = null,
    val company: String? = null,
    val school: String? = null,
    val city: String? = null,
    val height: Double? = null,
    val drinking: String? = null,
    val smoking: String? = null,
    val religion: String? = null,
    val interests: List<String> = emptyList(),
    val distance: Double? = null,
    val isVisible: Boolean = true,
    val showAge: Boolean = true,
    val showDistance: Boolean = true,
)

/** A paid message to someone the sender hasn't matched with, from the viewer's side. */
@Serializable
data class MessageRequestDto(
    /** "pending", "accepted" or "declined". */
    val status: String = "pending",
    /** True for the person who paid to send it. */
    val outgoing: Boolean = false,
    /** Messages the sender can still send before a reply. */
    val remaining: Int = 0,
)

@Serializable
data class RelationshipDto(
    val liked: Boolean = false,
    val isMutual: Boolean = false,
    /** Whether the viewer can pay to message this person without a match. */
    val acceptsMessageRequests: Boolean = true,
    val messageRequest: MessageRequestDto? = null,
)

/** The {user, profile, photos} envelope every person-listing endpoint returns. */
@Serializable
data class PersonCardDto(
    val user: UserDto,
    val profile: ProfileDto? = null,
    val photos: List<PhotoDto> = emptyList(),
    val relationship: RelationshipDto? = null,
    /** Profile views only: when this person last viewed the user. */
    val viewedAt: String? = null,
)

@Serializable
data class DiscoverResponse(val users: List<PersonCardDto> = emptyList())

@Serializable
data class MatchesResponse(val matches: List<PersonCardDto> = emptyList())

@Serializable
data class LockedPeopleResponse(
    val locked: Boolean = true,
    val count: Int = 0,
    val likes: List<PersonCardDto> = emptyList(),
    val views: List<PersonCardDto> = emptyList(),
)

@Serializable
data class LikeResponse(
    val liked: Boolean = true,
    val isMutual: Boolean = false,
    val alreadyLiked: Boolean = false,
    /** Free likes left today; null with Plus (no limit). */
    val likesLeft: Int? = null,
)

// ---------- My profile ----------

@Serializable
data class MyProfileResponse(
    val user: UserDto? = null,
    val profile: ProfileDto? = null,
    val photos: List<PhotoDto> = emptyList(),
)

@Serializable
data class CompleteProfileBody(
    val fullName: String,
    val dateOfBirth: String,
    val gender: String,
    val segment: String,
    val bio: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class UpdateProfileBody(
    val fullName: String? = null,
    val bio: String? = null,
    val city: String? = null,
    val occupation: String? = null,
    val education: String? = null,
    val interests: List<String>? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

// ---------- Chat ----------

@Serializable
data class MessageDto(
    @SerialName("_id") val id: String,
    val conversationId: String = "",
    val senderId: String,
    val receiverId: String,
    val content: String,
    val isRead: Boolean = false,
    val createdAt: String,
)

@Serializable
data class MessagesResponse(val messages: List<MessageDto> = emptyList(), val request: MessageRequestDto? = null)

@Serializable
data class ConversationUserDto(@SerialName("_id") val id: String, val fullName: String? = null)

@Serializable
data class ConversationDto(
    val conversationId: String,
    val otherUser: ConversationUserDto? = null,
    val lastMessage: MessageDto,
    val unreadCount: Int = 0,
    /** Set while the two are talking through a message request, not a match. */
    val request: MessageRequestDto? = null,
)

@Serializable
data class ConversationsResponse(val conversations: List<ConversationDto> = emptyList())

@Serializable
data class SendRequestBody(val content: String)

@Serializable
data class SendRequestResponse(
    /** They already liked the sender, so it became a match instead of a request. */
    val matched: Boolean = false,
    val request: MessageRequestDto? = null,
    val charged: Int = 0,
    /** Which free allowance paid for it: "free" (weekly) or "plus"; null if coins did. */
    val freeAllowance: String? = null,
    val balance: Int = 0,
)

@Serializable
data class UnreadCountResponse(val unreadCount: Int = 0)

// ---------- Coins ----------

@Serializable
data class CoinPackageDto(
    val id: String,
    val productId: String,
    val coins: Int,
    val priceUsd: Double,
    val best: Boolean = false,
)

@Serializable
data class CoinCostsDto(
    val messageRequest: Int = 200,
    val insights7: Int = 700,
    val insights30: Int = 1500,
    /** A date post beyond the free active ones. */
    val datePost: Int = 100,
)

// ---------- Relun Plus ----------

@Serializable
data class PlusDto(
    val active: Boolean = false,
    /** "weekly" or "monthly". */
    val plan: String = "monthly",
    /** "paystack" (renewing plan), "paystack_pass" (one-time) or "play". */
    val source: String = "play",
    val until: String,
    val autoRenew: Boolean = false,
)

@Serializable
data class PlusPlanDto(
    val id: String,
    val days: Int,
    /** Web price in minor units (kobo); the app shows Google Play's price when it has one. */
    val amount: Int,
    val currency: String = "NGN",
    val playProductId: String = "relun_plus",
    val playBasePlanId: String,
)

/** Free likes or message requests left; left is null when unlimited (Plus likes). */
@Serializable
data class AllowanceDto(val limit: Int? = null, val left: Int? = null, val resetAt: String? = null)

/** dates: free active date posts left; no reset, a slot frees when a post passes or is deleted. */
@Serializable
data class AllowancesDto(
    val likes: AllowanceDto = AllowanceDto(),
    val requests: AllowanceDto = AllowanceDto(),
    val dates: AllowanceDto = AllowanceDto(limit = 1, left = 1),
)

/** What Plus includes, from the server's settings, so copy never hard-codes it. */
@Serializable
data class PlusPerksDto(val monthlyRequests: Int = 5, val activeDates: Int = 3)

/** Plus status and the free allowances. GET /api/plus, and part of the wallet. */
@Serializable
data class EntitlementsResponse(
    val plus: PlusDto? = null,
    val plusPerks: PlusPerksDto = PlusPerksDto(),
    val plans: List<PlusPlanDto> = emptyList(),
    val allowances: AllowancesDto = AllowancesDto(),
)

@Serializable
data class PlayPlusBody(val purchaseToken: String, val basePlanId: String?)

@Serializable
data class InsightsBody(val days: Int)

@Serializable
data class WalletResponse(
    val balance: Int = 0,
    val insightsActive: Boolean = false,
    val insightsUntil: String? = null,
    val costs: CoinCostsDto = CoinCostsDto(),
    val packages: List<CoinPackageDto> = emptyList(),
    val pendingBonus: PendingBonusDto? = null,
    val plus: PlusDto? = null,
    val plusPerks: PlusPerksDto = PlusPerksDto(),
    val plans: List<PlusPlanDto> = emptyList(),
    val allowances: AllowancesDto = AllowancesDto(),
)

/** Coins the user was given but hasn't been told about yet. kind: "signup" or "gift". */
@Serializable
data class PendingBonusDto(val id: String, val coins: Int, val kind: String = "gift")

@Serializable
data class PurchaseBody(val productId: String, val purchaseToken: String, val platform: String = "android")

@Serializable
data class PurchaseResponse(val credited: Int = 0, val balance: Int = 0, val alreadyProcessed: Boolean = false)


@Serializable
data class InsightsResponse(val insightsActive: Boolean = true, val insightsUntil: String? = null, val balance: Int = 0)

// ---------- Dates ----------

@Serializable
data class DateRequestDto(
    val id: String,
    val status: String,
    val requester: PersonCardDto? = null,
)

@Serializable
data class DatePostDto(
    val id: String,
    val activity: String,
    val place: String,
    val scheduledFor: String,
    val description: String? = null,
    val createdAt: String? = null,
    val owner: PersonCardDto? = null,
    val myRequestStatus: String? = null,
    val requests: List<DateRequestDto> = emptyList(),
)

@Serializable
data class DatesResponse(val dates: List<DatePostDto> = emptyList())

@Serializable
data class CreateDateBody(
    val activity: String,
    val place: String,
    val scheduledFor: String,
    val description: String? = null,
)

@Serializable
data class CreateDateResponse(val date: DatePostDto, val charged: Int = 0, val balance: Int? = null)

@Serializable
data class RequestStatusBody(val status: String)

@Serializable
data class JoinResponse(val status: String = "pending", val alreadyRequested: Boolean = false)

// ---------- Safety & settings ----------

@Serializable
data class ReportBody(val reason: String = "other", val details: String? = null)

@Serializable
data class BlockedUserDto(
    val userId: String,
    val fullName: String,
    val photoUrl: String? = null,
)

@Serializable
data class BlocksResponse(val blocks: List<BlockedUserDto> = emptyList())

@Serializable
data class SettingsDto(
    val isVisible: Boolean = true,
    val showAge: Boolean = true,
    val showDistance: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val allowMessageRequests: Boolean = true,
    val maxDistanceKm: Int = 35,
    val ageMin: Int = 18,
    val ageMax: Int = 99,
    val segment: String = "relationship",
)

@Serializable
data class SettingsResponse(val settings: SettingsDto)

@Serializable
data class SettingsPatch(
    val isVisible: Boolean? = null,
    val showAge: Boolean? = null,
    val showDistance: Boolean? = null,
    val notificationsEnabled: Boolean? = null,
    val allowMessageRequests: Boolean? = null,
    val maxDistanceKm: Int? = null,
    val ageMin: Int? = null,
    val ageMax: Int? = null,
)

@Serializable
data class PushTokenBody(val token: String, val platform: String = "android", val provider: String = "fcm")

// ---------- Errors ----------

@Serializable
data class ApiErrorBody(
    val error: String? = null,
    val code: String? = null,
    val errors: List<FieldError>? = null,
    val required: Int? = null,
    val balance: Int? = null,
)

@Serializable
data class FieldError(val msg: String? = null)
