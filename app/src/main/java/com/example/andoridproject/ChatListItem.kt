import android.os.Parcel
import android.os.Parcelable

data class ChatListItem(
    val buyerId: String,
    val sellerId: String,
    val key: Long,
    val title: String  // title 필드 추가
) : Parcelable {
    // Parcelable constructor added to read from Parcel
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: ""  // title을 parcel에서 읽기
    )

    // Default constructor added for Parcelable
    constructor() : this("", "", 0L, "")  // 기본값으로 빈 문자열 사용

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(buyerId)
        parcel.writeString(sellerId)
        parcel.writeLong(key)
        parcel.writeString(title)  // title을 Long으로 쓰기
    }

    override fun describeContents(): Int {
        return 0
    }

    // Parcelable.Creator implementation modified to match the changes
    companion object CREATOR : Parcelable.Creator<ChatListItem> {
        override fun createFromParcel(parcel: Parcel): ChatListItem {
            return ChatListItem(parcel)
        }

        override fun newArray(size: Int): Array<ChatListItem?> {
            return arrayOfNulls(size)
        }
    }
}
