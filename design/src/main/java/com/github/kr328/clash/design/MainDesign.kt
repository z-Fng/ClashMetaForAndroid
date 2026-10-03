package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.util.trafficTotal
import com.github.kr328.clash.design.databinding.DesignAboutBinding
import com.github.kr328.clash.design.databinding.DesignMainBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.design.util.root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainDesign(context: Context) : Design<MainDesign.Request>(context) {
    enum class Request {
        ToggleStatus,
        OpenProxy,
        SetFollowConfigMode,
        SetRuleMode,
        SetGlobalMode,
        SetDirectMode,
        OpenProfiles,
        OpenProviders,
        OpenLogs,
        OpenSettings,
        OpenHelp,
        OpenAbout,
    }

    private val binding = DesignMainBinding
        .inflate(context.layoutInflater, context.root, false)

    private var currentOverrideMode: TunnelState.Mode? = null
    private var modeChanging = false

    override val root: View
        get() = binding.root

    suspend fun setProfileName(name: String?) {
        withContext(Dispatchers.Main) {
            binding.profileName = name
        }
    }

    suspend fun setClashRunning(running: Boolean) {
        withContext(Dispatchers.Main) {
            binding.clashRunning = running
            if (!running) modeChanging = false
            updateModeButtons()
        }
    }

    suspend fun setForwarded(value: Long) {
        withContext(Dispatchers.Main) {
            binding.forwarded = value.trafficTotal()
        }
    }

    suspend fun setMode(mode: TunnelState.Mode, overrideMode: TunnelState.Mode?) {
        withContext(Dispatchers.Main) {
            currentOverrideMode = overrideMode
            binding.mode = when (mode) {
                TunnelState.Mode.Direct -> context.getString(R.string.direct_mode)
                TunnelState.Mode.Global -> context.getString(R.string.global_mode)
                TunnelState.Mode.Rule -> context.getString(R.string.rule_mode)
                else -> context.getString(R.string.rule_mode)
            }
            updateModeButtons()
        }
    }

    suspend fun setModeChanging(changing: Boolean) {
        withContext(Dispatchers.Main) {
            modeChanging = changing
            updateModeButtons()
        }
    }

    private fun updateModeButtons() {
        val checkedId = when (currentOverrideMode) {
            null -> R.id.mode_follow_view
            TunnelState.Mode.Rule -> R.id.mode_rule_view
            TunnelState.Mode.Global -> R.id.mode_global_view
            TunnelState.Mode.Direct -> R.id.mode_direct_view
            else -> View.NO_ID
        }

        if (checkedId == View.NO_ID)
            binding.modeGroupView.clearCheck()
        else
            binding.modeGroupView.check(checkedId)

        val enabled = binding.clashRunning && !modeChanging
        binding.modeFollowView.isEnabled = enabled
        binding.modeRuleView.isEnabled = enabled
        binding.modeGlobalView.isEnabled = enabled
        binding.modeDirectView.isEnabled = enabled
    }

    suspend fun setHasProviders(has: Boolean) {
        withContext(Dispatchers.Main) {
            binding.hasProviders = has
        }
    }

    suspend fun showAbout(versionName: String) {
        withContext(Dispatchers.Main) {
            val binding = DesignAboutBinding.inflate(context.layoutInflater).apply {
                this.versionName = versionName
            }

            AlertDialog.Builder(context)
                .setView(binding.root)
                .show()
        }
    }

    init {
        binding.self = this

        binding.colorClashStarted = context.resolveThemedColor(com.google.android.material.R.attr.colorPrimary)
        binding.colorClashStopped = context.resolveThemedColor(R.attr.colorClashStopped)
    }

    fun request(request: Request) {
        when (request) {
            Request.SetFollowConfigMode,
            Request.SetRuleMode, Request.SetGlobalMode, Request.SetDirectMode -> {
                if (!binding.clashRunning || modeChanging) return

                modeChanging = true
                updateModeButtons()
            }
            else -> Unit
        }

        requests.trySend(request)
    }
}
