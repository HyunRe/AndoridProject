import android.os.Parcel
import android.os.Parcelable

data class SalesPost @JvmOverloads constructor(
    val sellerId: String = "",
    val title: String = "",
    val createdAt: Long = 0,
    val price: String = "",
    val content: String = "",
    val imageUrl: String = "",
    var like: ArrayList<String> = ArrayList(),
    var status: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        ArrayList<String>().apply {
            val size = parcel.readInt()
            for (i in 0 until size) {
                add(parcel.readString() ?: "")
            }
        },
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(sellerId)
        parcel.writeString(title)
        parcel.writeLong(createdAt)
        parcel.writeString(price)
        parcel.writeString(content)
        parcel.writeString(imageUrl)
        parcel.writeInt(like.size)
        like.forEach { value ->
            parcel.writeString(value)
        }
        parcel.writeByte(if (status) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SalesPost> {
        override fun createFromParcel(parcel: Parcel): SalesPost {
            return SalesPost(parcel)
        }

        override fun newArray(size: Int): Array<SalesPost?> {
            return arrayOfNulls(size)
        }
    }
}
