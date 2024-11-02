package com.saveourtool.save.demo.entity

import com.saveourtool.common.demo.RunCommandMap
import com.saveourtool.common.spring.entity.BaseEntity
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * Entity that encapsulates the run command that should be used to run specific [demo] mode
 *
 * @property demo [Demo] entity
 * @property modeName name of a mode, e.g. Fix or Warn
 * @property command command that runs the [demo] with mode with name [modeName]
 */
@Entity
class RunCommand(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demo_id")
    var demo: Demo,
    var modeName: String,
    var command: String,
) : BaseEntity()

/**
 * @return [RunCommandMap] where key is mode name and value is run command for that mode
 */
fun List<RunCommand>.toRunCommandsMap(): RunCommandMap = associate { mode ->
    mode.modeName to mode.command
}
