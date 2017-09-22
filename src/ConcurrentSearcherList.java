import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentSearcherList<T> {

/*  * Three kinds of threads share access to a singly-linked list:  * searchers, inserters and deleters. Searchers merely examine the list;  * hence they can execute concurrently with each other. Inserters add  * new items to the front of the list; insertions must be mutually
exclusive * to preclude two inserters from inserting new items at about  * the same time. However, one insert can proceed in parallel with  * any number of searches. Finally, deleters remove items from anywhere  * in the list. At most one deleter process can access the list at  * a time, and deletion must also be mutually exclusive with searches  * and insertions.  *  * Make sure that there are no data races between concurrent inserters and
searchers!  */

    private static class Node<T> { 
        final T item;
        Node<T> next;
        Node(T item, Node<T> next){
            this.item = item;
            this.next = next;
        }
    }
    
    // Make shared variable first volatile to eliminate data races
    private volatile Node<T> first;
    
    // Declare variables for number of actively executing inserters, removers and searchers
    private int ni = 0;
    private int nr = 0;
    private int ns = 0;
    
    //Declare Reentrant lock and associated condition variables for inserters, removers and searchers threads to wait on
    private final ReentrantLock lock;
    private final Condition insertcond;
    private final Condition removecond;
    private final Condition searchcond;
    
    public ConcurrentSearcherList() {
        first = null;
        // Initialize the lock and condition variables
        lock = new ReentrantLock();
        searchcond = lock.newCondition();
        insertcond = lock.newCondition();
        removecond = lock.newCondition();
        
    }

    /**  * Inserts the given item into the list.  *  * Precondition:  item != null  *  * @param item  * @throws InterruptedException
     * @param item 
     * @throws java.lang.InterruptedException */ 

    public void insert(T item) throws InterruptedException{
        assert item != null: "Error in ConcurrentSearcherList insert: Attempt to insert null"; 
        start_insert(); 
        try{
            first = new Node<T>(item, first);
        } 
        finally{
            end_insert();
        }
    }

    /**  * Determines whether or not the given item is in the list  *  * Precondition:  item != null  ** @param item  * @return  true if item is in the list, false otherwise.  * @throws InterruptedException
     * @param item 
     * @return  
     * @throws java.lang.InterruptedException */ 
    
    public boolean search(T item) throws InterruptedException{
        assert item != null: "Error in ConcurrentSearcherList insert: Attempt to search for null";
        start_search(); 
        try{
            for(Node<T> curr = first;  curr != null ; curr = curr.next){
                if (item.equals(curr.item)) 
                    return true;
            } 
            return false;
        }
        finally{
            end_search();
        }
    }

    /**  * Removes the given item from the list if it exists.  Otherwise the list is not modified. * The return value indicates whether or not the item was removed.  *  * Precondition:  item != null.  *  * @param item  * @return  whether or not item was removed from the list.  * @throws InterruptedException
     * @param item 
     * @return  
     * @throws java.lang.InterruptedException */ 
    
    public boolean remove(T item) throws InterruptedException{
        assert item != null: "Error in ConcurrentSearcherList insert: Attempt to remove null"; 
        start_remove();
        try{
            if(first == null) return false;
            if (item.equals(first.item))
            {first = first.next; return true;}
            for(Node<T> curr = first;  curr.next != null ; curr = curr.next){
                if (item.equals(curr.next.item)) { curr.next = curr.next.next;
                return true;
                }
            }
            return false;
        }
        finally{
            end_remove();
        }
    }
    
    private void start_insert() throws InterruptedException{
        lock.lock();
        try{
        // <await((nr == 0) && (ni==0)) ni++>    
        while(!((nr == 0) && (ni == 0))) {
            insertcond.await();
        }
        ni++;
        }
        finally {lock.unlock();}
    } 
    
    private void end_insert(){
        lock.lock();
        try{
            // <ni-->
            ni--;
            if (ni == 0) {
                // Signal all the threads waiting on removecond and insertcond for number of actively executing inserters to become zero
                removecond.signalAll();
                insertcond.signalAll();
            } 
        }
        finally {lock.unlock();}
    } 
    
    
    private void start_search() throws InterruptedException{
        lock.lock();
        try{
        // <await(nr == 0) ns++>
        while(!(nr == 0)) {
            searchcond.await();
        }
        ns++;
        }
        finally {lock.unlock();}
    } 
    
    private void end_search(){
        lock.lock();
        try {
            // <ns-->
            ns--;
            //Signal all the threads waiting on removecond for number of actively executing searchers to become zero
            if (ns == 0) removecond.signalAll();
        }
        finally {lock.unlock();}
    }
        
    
    private void start_remove() throws InterruptedException {
        lock.lock();
        try{
           // <await((nr == 0) && (ni == 0) && (ns == 0)) nr++>
           while(!((nr == 0) && (ni == 0) && (ns == 0)))
               removecond.await();
           nr++;
        }
        finally { lock.unlock();}
    } 
    
    private void end_remove() {
        lock.lock();
        try {
            // <nr-->
            nr--;
            if (nr == 0) {
                //Signal all the threads waiting on removecond,searchcond and insertcond for number of actively executing removers to become zero
                removecond.signalAll();
                searchcond.signalAll();
                insertcond.signalAll();
            }
        }
        finally { lock.unlock();}
    }
    
    
}

