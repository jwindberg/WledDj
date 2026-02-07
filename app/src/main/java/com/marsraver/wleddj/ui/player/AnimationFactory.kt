package com.marsraver.wleddj.ui.player

import android.content.Context
import com.marsraver.wleddj.animations.*
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.model.AnimationRegion
import com.marsraver.wleddj.model.SavedAnimation
import com.marsraver.wleddj.model.AnimationType
import com.marsraver.wleddj.engine.color.Palette

object AnimationFactory {

    fun createAnimation(type: AnimationType, context: Context): Animation {
        return type.create(context)
    }
    
    fun createSavedAnimation(region: AnimationRegion): SavedAnimation {
        val type = AnimationType.fromInstance(region.animation)
        
        // Palette is now an Enum, so we can directly access it from the animation
        val palette = region.animation.currentPalette

        return SavedAnimation(
            id = region.id,
            type = type,
            rectLeft = region.rect.left,
            rectTop = region.rect.top,
            rectRight = region.rect.right,
            rectBottom = region.rect.bottom,
            rotation = region.rotation,
            text = if (region.animation.supportsText()) region.animation.getText() else null,
            primaryColor = if (region.animation.supportsPrimaryColor()) region.animation.primaryColor else null,
            secondaryColor = if (region.animation.supportsSecondaryColor()) region.animation.secondaryColor else null,
            palette = palette
        )
    }

    fun getAvailableAnimations(): List<AnimationMetadata> {
        return AnimationType.entries.filter { it != AnimationType.UNKNOWN }.sortedBy { it.displayName }.map { type ->
            AnimationMetadata(type, type.isAudioReactive)
        }
    }

    data class AnimationMetadata(val type: AnimationType, val isAudioReactive: Boolean) {
        val name get() = type.displayName
    }
}
