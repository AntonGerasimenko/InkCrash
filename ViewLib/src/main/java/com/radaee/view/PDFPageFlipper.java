package com.radaee.view;

import android.os.Handler;
import android.os.Message;

/**
 * Created by radaee on 2015/3/13.
 */
public class PDFPageFlipper
{
    private PDFPageView m_cur;
    private PDFPageView m_back;
    private VThread m_thread;
    private Handler m_hand;
    private final void init()
    {
        m_hand = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what)//render finished.
                {
                    case 0:
                        //if( m_listener != null ) m_listener.OnPDFInvalidate(false);
                        break;
                    case 1://find operation returned.
                    {
                        if (msg.arg1 == 1)//succeeded
                        {
                            //PDFPageView.this.vFindGoto();
                            //if( m_listener != null )
                            //    m_listener.OnPDFFound( true );
                        } else {
                            //if( m_listener != null )
                            //    m_listener.OnPDFFound( false );
                        }
                    }
                    break;
                    case 100://timer
                        //if( m_scroller.isFinished() && m_pages != null && m_status != STA_ZOOM )
                        //    vOnTimer(msg.obj);
                        break;
                }
                super.handleMessage(msg);
            }
        };
        m_thread = new VThread(m_hand);
    }
    private void uninit()
    {
        m_thread.destroy();
    }
}
