package com.daemon.liveeventbus.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.daemon.liveeventbus.EventObserver
import com.daemon.liveeventbus.LiveEventBus
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment1, FirstFragment())
                .commit()

        supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment2, SecondFragment())
                .commit()

        val ob1 = object : EventObserver<MainActivityEvent>() {
            override fun onEvent(event: MainActivityEvent) {
                info.text = "收到：MainActivityEvent\n${info.text}"
            }
        }
        send1.setOnClickListener { LiveEventBus.emit(MainActivityEvent) }
        reg1.setOnClickListener { LiveEventBus.with(MainActivityEvent::class.java).observe(this, ob1) }
        ureg1.setOnClickListener { LiveEventBus.with(MainActivityEvent::class.java).removeObserver(ob1) }

        val ob2 = object : EventObserver<FirstFragmentEvent>() {
            override fun onEvent(event: FirstFragmentEvent) {
                info.text = "收到：FirstFragmentEvent\n${info.text}"
            }
        }
        send2.setOnClickListener { LiveEventBus.emit(FirstFragmentEvent) }
        reg2.setOnClickListener { LiveEventBus.with(FirstFragmentEvent::class.java).observe(this, ob2) }
        ureg2.setOnClickListener { LiveEventBus.with(FirstFragmentEvent::class.java).removeObserver(ob2) }

        val ob3 = object : EventObserver<SecondFragmentEvent>() {
            override fun onEvent(event: SecondFragmentEvent) {
                info.text = "收到：SecondFragmentEvent\n${info.text}"
            }
        }
        send3.setOnClickListener { LiveEventBus.emit(SecondFragmentEvent) }
        reg3.setOnClickListener { LiveEventBus.with(SecondFragmentEvent::class.java).observe(this, ob3) }
        ureg3.setOnClickListener { LiveEventBus.with(SecondFragmentEvent::class.java).removeObserver(ob3) }
    }
}
