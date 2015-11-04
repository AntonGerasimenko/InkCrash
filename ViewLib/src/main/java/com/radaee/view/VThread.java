package com.radaee.view;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.Timer;
import java.util.TimerTask;

public class VThread extends Thread
{
	private Handler m_hand = null;
	private Handler m_handUI = null;
	private Timer m_timer;
	private TimerTask m_timer_task = null;
	private boolean is_notified = false;
	private boolean is_waitting = false;
	private synchronized void wait_init()
	{
		try
		{
			if( is_notified )
				is_notified = false;
			else
			{
				is_waitting = true;
				wait();
				is_waitting = false;
			}
		}
		catch(Exception e)
		{
		}
	}
	private synchronized void notify_init()
	{
		if( is_waitting )
			notify();
		else
			is_notified = true;
	}
	protected VThread(Handler hand_ui)
	{
		super();
		m_handUI = hand_ui;
	}
	@Override
    public void start()
	{
		super.start();
		wait_init();
		m_timer = new Timer();
		m_timer_task = new TimerTask()
        {
        	public void run()
        	{
        		m_handUI.sendEmptyMessage(100);
        	}
        };
        m_timer.schedule(m_timer_task, 100, 100);
	}
	@Override
    public void run()
	{
		Looper.prepare();
		m_hand = new Handler(Looper.myLooper())
		{
			public void handleMessage(Message msg)
			{
				if( msg != null )
				{
					if( msg.what == 0 )//render function
					{
						((VCache)msg.obj).vRender();
						m_handUI.sendMessage( m_handUI.obtainMessage(0, (VCache)msg.obj ) );
						msg.obj = null;
						super.handleMessage(msg);
					}
					else if( msg.what == 1 )
					{
						((VCache)msg.obj).vDestroy();
						msg.obj = null;
						super.handleMessage(msg);
					}
					else if( msg.what == 2 )
					{
						int ret = ((VFinder)msg.obj).find();
						m_handUI.sendMessage( m_handUI.obtainMessage(1, ret, 0) );
						msg.obj = null;
						super.handleMessage(msg);
					}
                    else if( msg.what == 3 )
                    {
                        VPageCache.VBlock blk = (VPageCache.VBlock)msg.obj;
                        blk.Render();
                    }
                    else if( msg.what == 4 )
                    {
                        VPageCache.VBlock blk = (VPageCache.VBlock)msg.obj;
                        blk.Reset();
                    }
					else if( msg.what == 100 )//quit
					{
						super.handleMessage(msg);
						getLooper().quit();
					}
				}
				else
                {
                    getLooper().quit();
                }
			}
		};
		notify_init();
		Looper.loop();
	}
	protected void start_render( VCache cache )
	{
		if( cache.vStart() )
			m_hand.sendMessage(m_hand.obtainMessage(0, cache));
	}
	protected void end_render( VCache cache )
	{
		if( cache.vEnd() )
			m_hand.sendMessage(m_hand.obtainMessage(1, cache));
	}
	protected void start_find( VFinder finder )
	{
		m_hand.sendMessage(m_hand.obtainMessage(2, finder));
	}
    protected void start_render_cache(VPageCache pc, int blk)
    {
        VPageCache.VBlock dst = pc.blk_render(blk);
        if(dst != null) m_hand.sendMessage(m_hand.obtainMessage(3, dst));
    }
    protected void end_render_cache(VPageCache pc, int blk)
    {
        VPageCache.VBlock dst = pc.blk_cancel(blk);
        if(dst != null) m_hand.sendMessage(m_hand.obtainMessage(4, dst));
    }
	public synchronized void destroy()
	{
		try
		{
			m_timer.cancel();
			m_timer_task.cancel();
			m_timer = null;
			m_timer_task = null;
			m_hand.sendEmptyMessage(100);
			join();
			m_hand = null;
			m_handUI = null;
		}
		catch(InterruptedException e)
		{
		}
	}
}
