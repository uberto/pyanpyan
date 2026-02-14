package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.ChecklistId
import com.pyanpyan.domain.model.ChecklistItemId
import com.pyanpyan.domain.model.ItemIconId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ChecklistIdSerializer : KSerializer<ChecklistId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ChecklistId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ChecklistId) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ChecklistId {
        return ChecklistId(decoder.decodeString())
    }
}

object ChecklistItemIdSerializer : KSerializer<ChecklistItemId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ChecklistItemId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ChecklistItemId) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ChecklistItemId {
        return ChecklistItemId(decoder.decodeString())
    }
}

object ItemIconIdSerializer : KSerializer<ItemIconId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ItemIconId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ItemIconId) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ItemIconId {
        return ItemIconId(decoder.decodeString())
    }
}
