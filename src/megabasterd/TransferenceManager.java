package megabasterd;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import static java.util.logging.Logger.getLogger;
import javax.swing.JPanel;
import static megabasterd.MainPanel.THREAD_POOL;
import static megabasterd.MiscTools.swingReflectionInvoke;

/**
 *
 * @author tonikelope
 */
abstract public class TransferenceManager implements Runnable, SecureNotifiable {
    
    private final ConcurrentLinkedQueue<Transference> _transference_provision_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_waitstart_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_remove_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_finished_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_running_list;
    private final javax.swing.JPanel _scroll_panel;
    private final javax.swing.JLabel _status;
    private final javax.swing.JButton _close_all_button;
    private final javax.swing.JButton _pause_all_button;
    private final javax.swing.MenuElement _clean_all_menu;
    private final int _max_running_trans;
    private final MainPanel _main_panel;
    private final Object _secure_notify_lock;
    private boolean _notified;
    private volatile boolean _removing_transferences;
    private volatile boolean _provisioning_transferences;
    private volatile boolean _starting_transferences;
    private volatile boolean _preprocessing_transferences;

    abstract public void provision(Transference transference);
    
    abstract public void remove(Transference transference);
    
    public boolean isRemoving_transferences() {
        return _removing_transferences;
    }

    public void setRemoving_transferences(boolean removing) {
        _removing_transferences = removing;
    }

    public boolean isProvisioning_transferences() {
        return _provisioning_transferences;
    }

    public void setProvisioning_transferences(boolean provisioning) {
        _provisioning_transferences = provisioning;
    }

    public boolean isStarting_transferences() {
        return _starting_transferences;
    }

    public void setStarting_transferences(boolean starting) {
        _starting_transferences = starting;
    }

    public void setPreprocessing_transferences(boolean preprocessing) {
        _preprocessing_transferences = preprocessing;
    }
 
    public TransferenceManager(MainPanel main_panel, int max_running_trans, javax.swing.JLabel status, javax.swing.JPanel scroll_panel, javax.swing.JButton close_all_button, javax.swing.JButton pause_all_button, javax.swing.MenuElement clean_all_menu) {
        _notified = false;
        _removing_transferences = false;
        _provisioning_transferences = false;
        _starting_transferences=false;
        _preprocessing_transferences=false;
        _main_panel = main_panel;
        _max_running_trans = max_running_trans;
        _scroll_panel = scroll_panel;
        _status = status;
        _close_all_button = close_all_button;
        _pause_all_button = pause_all_button;
        _clean_all_menu = clean_all_menu;
        _secure_notify_lock = new Object();
        _transference_waitstart_queue = new ConcurrentLinkedQueue();
        _transference_provision_queue = new ConcurrentLinkedQueue();
        _transference_remove_queue = new ConcurrentLinkedQueue();
        _transference_finished_queue = new ConcurrentLinkedQueue();
        _transference_running_list = new ConcurrentLinkedQueue();
    }
    
    @Override
    public void secureNotify()
    {
        synchronized(_secure_notify_lock) {
      
            _notified = true;

            _secure_notify_lock.notify();
        }
    }
    
    @Override
    public void secureWait() {
        
        synchronized(_secure_notify_lock)
        {
            while(!_notified) {

                try {
                    _secure_notify_lock.wait();
                } catch (InterruptedException ex) {
                    getLogger(TransferenceManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            _notified = false;
        }
    }
    
    @Override
    public void secureNotifyAll() {
        
        synchronized(_secure_notify_lock) {
            
            _notified = true;
      
            _secure_notify_lock.notifyAll();
        }
    }

    public MainPanel getMain_panel() {
        return _main_panel;
    }
    
    public ConcurrentLinkedQueue<Transference> getTransference_provision_queue() {
        return _transference_provision_queue;
    }

    public ConcurrentLinkedQueue<Transference> getTransference_start_queue() {
        return _transference_waitstart_queue;
    }

    public ConcurrentLinkedQueue<Transference> getTransference_remove_queue() {
        return _transference_remove_queue;
    }

    public ConcurrentLinkedQueue<Transference> getTransference_finished_queue() {
        return _transference_finished_queue;
    }

    public ConcurrentLinkedQueue<Transference> getTransference_running_list() {
        return _transference_running_list;
    }

    public JPanel getScroll_panel() {
        return _scroll_panel;
    }
    
    public void closeAllFinished() 
    {
        _transference_remove_queue.addAll(new ArrayList(_transference_finished_queue));
        
        _transference_finished_queue.clear();
        
        secureNotify();
    }
    
    public void closeAllWaiting() 
    {   
        _transference_remove_queue.addAll(new ArrayList(_transference_waitstart_queue));
        
        _transference_waitstart_queue.clear();
        
        secureNotify();
    }
    
    public void start(Transference transference) {
        
        _transference_running_list.add(transference);

        _scroll_panel.add((Component)transference.getView(), 0);
        
        transference.start();
        
        secureNotify();
    }
    
    public void pauseAll()
    {
        for(Transference transference:_transference_running_list) {
            
            if(!transference.isPaused()) {
                
                transference.pause();
            }
        }
      
        secureNotify();
    }
    
    public void sortTransferenceStartQueue() 
    {
        ArrayList<Transference> trans_list = new ArrayList(_transference_waitstart_queue);

        trans_list.sort(new Comparator<Transference> () {

        @Override
        public int compare(Transference o1, Transference o2) {
      
            return o1.getFile_name().compareToIgnoreCase(o2.getFile_name());
        }
        });
        
        _transference_waitstart_queue.clear();
        
        _transference_waitstart_queue.addAll(trans_list);
    }
    
    private void updateView() {
        
        if(!_transference_running_list.isEmpty()) {
            
            boolean show_pause_all = false;
            
            for(Transference trans:_transference_running_list) { 
                
                if((show_pause_all = !trans.isPaused())) {
                    
                    break;
                }
            }
            
            swingReflectionInvoke("setVisible", _pause_all_button, show_pause_all);
            
        } else {
            
            swingReflectionInvoke("setVisible", _pause_all_button, false);
        }
  
        swingReflectionInvoke("setEnabled", _clean_all_menu, !_transference_waitstart_queue.isEmpty());
 
        if(!_transference_finished_queue.isEmpty()) {

            swingReflectionInvoke("setText", _close_all_button, "Close all finished ("+_transference_finished_queue.size()+")" );
            
            swingReflectionInvoke("setVisible", _close_all_button, true);
            
        } else {
            
            swingReflectionInvoke("setVisible", _close_all_button, false);
        }
        
        if(!_preprocessing_transferences) {
            swingReflectionInvoke("setText", _status, genStatus());
        }
    }
    
    private String genStatus() {
        
        int prov = _transference_provision_queue.size();
        
        int rem = _transference_remove_queue.size();
        
        int wait = _transference_waitstart_queue.size();
        
        int run = _transference_running_list.size();
        
        int finish = _transference_finished_queue.size();

        return (prov+rem+wait+run+finish > 0)?"Prov: "+prov+" / Rem: "+rem+" / Wait: "+wait+" / Run: "+run+" / Finish: "+finish:"";
    }
    
    @Override
    public void run() {

        while(true)
        {
            if(!isProvisioning_transferences() && !getTransference_provision_queue().isEmpty())
            {
                setProvisioning_transferences(true);
                
                THREAD_POOL.execute(new Runnable(){
                                @Override
                                public void run(){
        
                                    while(!getTransference_provision_queue().isEmpty())
                                    {
                                        final Transference transference = getTransference_provision_queue().poll();

                                        if(transference != null) {
                                            
                                            provision(transference);

                                            }
                                        }
                                    
                                    setProvisioning_transferences(false);
                                    
                                    secureNotify();
             
                                }});

            }
            
            if(!isRemoving_transferences() && !getTransference_remove_queue().isEmpty()) {
                
                setRemoving_transferences(true);
                
                THREAD_POOL.execute(new Runnable(){
                                @Override
                                public void run(){
                                
                                   
                                    while(!getTransference_remove_queue().isEmpty()) {

                                        Transference transference = getTransference_remove_queue().poll();

                                        if(transference != null) {
                                            remove(transference);
                                        }
                                    }
                                    
                                    setRemoving_transferences(false);
                                    
                                    secureNotify();
                                    
                                }});
            }
            
            if(!isStarting_transferences() && !getTransference_start_queue().isEmpty() && getTransference_running_list().size() < _max_running_trans)
            {
                setStarting_transferences(true);
                
                THREAD_POOL.execute(new Runnable(){
                                @Override
                                public void run(){
                                
                                while(!getTransference_start_queue().isEmpty() && getTransference_running_list().size() < _max_running_trans) {
                
                                    Transference transference = getTransference_start_queue().poll();

                                    if(transference != null) {

                                        start(transference);
                                    }
                                }
                                
                                setStarting_transferences(false);
                                    
                                secureNotify();
                                
                           }});
            }
            
            updateView();

            secureWait();
        }
        
        }
    
}
