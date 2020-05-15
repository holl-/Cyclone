package systemcontrol

import javafx.application.Platform
import javafx.scene.robot.Robot
import mediacommand.JIntellitypeMediaCommandManager
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

abstract class LocalMachine {
    abstract fun setPreventStandby(preventStandby: Boolean, source: Any)
    abstract fun enterStandby(): Boolean
    abstract fun turnOffMonitors(): Boolean

    private class WindowsMachine : LocalMachine()
    {
        private val standbyPreventionInterval: Long = 115
        private val standbyPreventers = HashSet<Any>()
        private var standbyPrevention: ScheduledExecutorService? = null
        private var robot: Robot? = null
        private var initializingRobot = false

        override fun setPreventStandby(preventStandby: Boolean, source: Any) {
            synchronized(this) {
                if (preventStandby) standbyPreventers.add(source)
                else standbyPreventers.remove(source)
                if (standbyPreventers.isNotEmpty() && standbyPrevention == null) {
                    if (robot == null && !initializingRobot) {
                        initializingRobot = true
                        Platform.runLater { robot = Robot() }
                    }
                    standbyPrevention = Executors.newSingleThreadScheduledExecutor()
                    standbyPrevention!!.scheduleAtFixedRate({ Platform.runLater {
                        val robot = this.robot
                        if (robot != null) {
                            val position = robot.mousePosition
                            robot.mouseMove(position.add(1.0, 0.0));
                            robot.mouseMove(position);
                        }
                    } }, standbyPreventionInterval, standbyPreventionInterval, TimeUnit.SECONDS);
                }
                if (standbyPreventers.isEmpty() && standbyPrevention != null) {
                    standbyPrevention?.shutdown()
                    standbyPrevention = null
                }
            }
        }

        override fun enterStandby(): Boolean {
            val windir = System.getenv("windir")
            val command = "$windir/System32/rundll32.exe powrprof.dll,SetSuspendState"
            return try {
                Runtime.getRuntime().exec(command)
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }

        override fun turnOffMonitors(): Boolean {
            val exe = JIntellitypeMediaCommandManager.getBinaryApplicationFile(LocalMachine::class.java, "Turn Off Monitor.exe")
            return try {
                Runtime.getRuntime().exec(exe.absolutePath)
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }
    }

    companion object {
        private var LOCAL_MACHINE: LocalMachine? = null

        private fun createLocalMachine(): LocalMachine? {
            val os = System.getProperty("os.name")
            return if (os.toLowerCase().contains("windows")) {
                WindowsMachine()
            } else null
        }

        @JvmStatic
        fun getLocalMachine(): LocalMachine? {
            if (LOCAL_MACHINE == null) LOCAL_MACHINE = createLocalMachine()
            return LOCAL_MACHINE
        }
    }
}
