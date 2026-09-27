package ru.larpinovplay.finniapp.presentation.adventure

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.adventure.model.Adventure

/**
 * Как выглядит приключение: своя эмблема и своё «небо» — два цвета фона сверху вниз, и оттенок наклеек.
 * Так каждое приключение недели узнаётся с первого взгляда и в заданиях, и на своём экране.
 */
data class AdventureLook(@DrawableRes val emblem: Int, val skyTop: Color, val skyBottom: Color, val tint: Color)

val Adventure.look: AdventureLook
    get() = when (id) {
        "first_shop" -> AdventureLook(R.drawable.ic_adv_shop, Color(0xFFFFE8BE), Color(0xFFFFBFA0), Color(0xFFFFF0E6))
        "park_walk" -> AdventureLook(R.drawable.ic_adv_park, Color(0xFFDDF7EA), Color(0xFFA7E3C6), Color(0xFFE6F8F2))
        "scratched_paw" -> AdventureLook(R.drawable.ic_adv_paw, Color(0xFFFFE6F0), Color(0xFFFFBBD2), Color(0xFFFFE6F0))
        "fair" -> AdventureLook(R.drawable.ic_adv_fair, Color(0xFFFFF3C4), Color(0xFFFFC6D9), Color(0xFFFFF5C9))
        "friend_gift" -> AdventureLook(R.drawable.ic_adv_gift, Color(0xFFE8EDFF), Color(0xFFC3CCFF), Color(0xFFE6EEFF))
        else -> AdventureLook(R.drawable.ic_adventure, Color(0xFFFFE8BE), Color(0xFFFFBFA0), Color(0xFFFFF0E6))
    }
