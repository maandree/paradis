/**
 *  Paradis — Ever growing network for parallel and distributed computing.
 *  Copyright © 2012  Mattias Andrée
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.nongnu.paradis;

import java.util.*;


/**
 * Client local message broadcasting blackboard
 *
 * @author  Mattias Andrée, <a href="mailto:maandree@kth.se">maandree@kth.se</a>
 */
@requires("java-environment>=7")
public class Blackboard
{
    /**
     * Do not thread
     */
    public static final ThreadingPolicy NO_THREADING = null;
    
    /**
     * Normal thread
     */
    public static final ThreadingPolicy THREADED;
    
    /**
     * Daemon thread
     */
    public static final ThreadingPolicy DAEMON_THREADING;
    
    /**
     * Nice thread
     */
    public static final ThreadingPolicy NICE_THREADING;
    
    /**
     * Nice daemon thread
     */
    public static final ThreadingPolicy NICE_DAEMON_THREADING;
    
    
    
    /**
     * Multiton constructor
     */
    private Blackboard()
    {
        // Do nothing
    }
    
    
    /**
     * Gets, and if missing, creates, an instance of this class
     * 
     * @param   name  The name (unique identifier) of the instance, {@code null} is default
     * @return        The, possibily, new instance
     */
    public static Blackboard getInstance(final Object name)
    {
        Blackboard instance;
        if ((instance = instances.get(name)) == null)
            synchronized (instances)
            {   if ((instance = instances.get(name)) == null)
                    instances.put(name, instance = new Blackboard());
            }
        return instance;
    }
    
    
    /**
     * The instance of this class
     */
    private static final HashMap<Object, Blackboard> instances = new HashMap<>();
    
    
    
    /**
     * Class initialiser
     */
    static
    {
        THREADED = new ThreadingPolicy()
                {   /**
                     * {@inheritDoc}
                     */
                    @Override
                    public Thread createThread(final Runnable runnable)
                    {   final Thread thread = new Thread(runnable);
                        thread.setDaemon(false);
                        thread.setPriority(5); //normal: 5 of 1..10; corresponding nice value: 0
                        return thread;
                }   };
        
        DAEMON_THREADING = new ThreadingPolicy()
                {   /**
                     * {@inheritDoc}
                     */
                    @Override
                    public Thread createThread(final Runnable runnable)
                    {   final Thread thread = new Thread(runnable);
                        thread.setDaemon(true);
                        thread.setPriority(5); //normal: 5 of 1..10; corresponding nice value: 0
                        return thread;
                }   };

        NICE_THREADING = new ThreadingPolicy()
                {   /**
                     * {@inheritDoc}
                     */
                    @Override
                    public Thread createThread(final Runnable runnable)
                    {   final Thread thread = new Thread(runnable);
                        thread.setDaemon(false);
                        thread.setPriority(2); //below normal: 2 of 1..10; corresponding nice value: 3
                        return thread;
                }   };
        
        NICE_DAEMON_THREADING = new ThreadingPolicy()
                {   /**
                     * {@inheritDoc}
                     */
                    @Override
                    public Thread createThread(final Runnable runnable)
                    {   final Thread thread = new Thread(runnable);
                        thread.setDaemon(true);
                        thread.setPriority(2); //below normal: 2 of 1..10; corresponding nice value: 3
                        return thread;
                }   };
    }
    
    
    
    /**
     * Registrered observers
     */
    private HashSet<BlackboardObserver> observers = new HashSet<>();
    
    /**
     * How to thread message observations
     */
    private HashMap<BlackboardObserver, HashMap<Class<? extends BlackboardMessage>, ThreadingPolicy>> observationThreading = new HashMap<>();
    
    /**
     * In which order should observers be notified
     */
    private HashMap<BlackboardObserver, HashMap<Class<? extends BlackboardMessage>, Integer>> observationPriorities = new HashMap<>();
    
    /**
     * Concurrency monitor
     */
    private Object monitor = new Object();
    
    
    
    /**
     * This interface is used for all event
     */
    public static interface BlackboardMessage
    {
        //Marker interface
    }
    
    
    /**
     * Message broadcasted when a new observer is registrered or when a observer is unregistrered
     */
    public static class ObserverRegisterMessage implements BlackboardMessage
    {
        /**
         * Constructor
         * 
         * @param  observer  The observer
         * @param  register  {@code true} if the observer is newly registered, {@code false} if newly unregistered
         */
        public ObserverRegisterMessage(final BlackboardObserver observer, final boolean register)
        {
            this.observer = observer;
            this.register = register;
        }
        
        
        
        /**
         * The observer
         */
        private final BlackboardObserver observer;
        
        /**
         * {@code true} if the observer is newly registered, {@code false} if newly unregistered
         */
        private final boolean register;
        
        
        
        /**
         * Gets the observer
         * 
         * @return  The observer
         */
        public BlackboardObserver getObserver()
        {   return this.observer;
        }
        
        /**
         * Gets whether the observer is newly registered or newly unregistered 
         * 
         * @return  {@code true} if the observer is newly registered, {@code false} if newly unregistered
         */
        public boolean getRegister()
        {   return this.register;
        }
        
        
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {   return this.observer.toString() + (this.register ? " registered" : " unregistered");
        }
        
    }
    
    
    /**
     * This interface makes observersion on the enclosing class possible
     */
    public static interface BlackboardObserver
    {
        /**
         * This method is invoked when the a message is pinned on the blackboard
         * 
         * @param  message  The broadcasted message
         */
        public void messageBroadcasted(final Blackboard.BlackboardMessage message);
    }
    
    
    /**
     * Message observation threading policy
     */
    public static interface ThreadingPolicy
    {
        /**
         * Creates a thread according to the policy
         *
         * @param   runnable  The {@code run} implementation of the thread
         * @return            The new thread
         */
        public Thread createThread(final Runnable runnable);
    }
    
    
    
    /**
     * Registers a message type-wide observer
     *
     * @param  observer  The observer to register
     */
    public void registerObserver(final BlackboardObserver observer)
    {
        synchronized (this.monitor)
        {
            System.err.println("BLACKBOARD.registerObserver(" + observer + ")");
            this.observers.add(observer);
            this.broadcastMessage(new ObserverRegisterMessage(observer, true));
        }
    }
    
    
    /**
     * Unregisters a message type-wide observer
     *
     * @param  observer  The observer to unregister
     */
    public void unregisterObserver(final BlackboardObserver observer)
    {
        synchronized (this.monitor)
        {
            System.err.println("BLACKBOARD.unregisterObserver(" + observer + ")");
            this.observers.remove(observer);
            this.observationThreading.remove(observer);
            this.observationPriorities.remove(observer);
            this.broadcastMessage(new ObserverRegisterMessage(observer, false));
        }
    }
    
    
    /**
     * Registers a threading policy for an observer and a message type.<br/>
     * If a threading policy is registrered for an observer it will only receive message with a registrered threading policy.
     * 
     * @param  observer     The observer
     * @param  messageType  The message type
     * @param  policy       The threading policy
     * 
     * @deprecated  Use overloading {@link #registerThreadingPolicy(BlackboardObserver, ThreadingPolicy, Class...)} instead
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public void registerThreadingPolicy(final BlackboardObserver observer, final Class<? extends BlackboardMessage> messageType, final ThreadingPolicy policy)
    {
        registerThreadingPolicy(observer, policy, messageType);
    }
    
    
    /**
     * Registers a threading policy for an observer and some message types.<br/>
     * If a threading policy is registrered for an observer it will only receive message with a registrered threading policy.
     * 
     * @param  observer      The observer
     * @param  policy        The threading policy
     * @param  messageTypes  The message types, must be {@link Class}<code>&lt;? extends </code>{@link BlackboardMessage}<code>&gt;</code>
     */
    @SuppressWarnings("unchecked")
    public void registerThreadingPolicy(final BlackboardObserver observer, final ThreadingPolicy policy, final Class<? extends BlackboardMessage>... messageTypes)
    {
        synchronized (this.monitor)
        {
            HashMap<Class<? extends BlackboardMessage>, ThreadingPolicy> map = this.observationThreading.get(observer);
            if (map == null)
            {
                map = new HashMap<Class<? extends BlackboardMessage>, ThreadingPolicy>();
                this.observationThreading.put(observer, map);
            }
            for (final Class<? extends BlackboardMessage> messageType : messageTypes)
                map.put(messageType, policy);
        }
    }
    
    
    /**
     * Registers a priority for an observer and some message types
     * 
     * @param  observer      The observer
     * @param  nice          The priority, zero is default, and positive is low priority (that is, executed later that negative)
     * @param  messageTypes  The message types, must be {@link Class}<code>&lt;? extends </code>{@link BlackboardMessage}<code>&gt;</code>,
     *                       you can used {@code null} to set a default for the observer
     */
    @SuppressWarnings("unchecked")
    public void registerPriority(final BlackboardObserver observer, final int nice, final Class<? extends BlackboardMessage>... messageTypes)
    {
        synchronized (this.monitor)
        {
            HashMap<Class<? extends BlackboardMessage>, Integer> map = this.observationPriorities.get(observer);
            if (map == null)
            {
                map = new HashMap<Class<? extends BlackboardMessage>, Integer>();
                this.observationPriorities.put(observer, map);
            }
            for (final Class<? extends BlackboardMessage> messageType : messageTypes)
                map.put(messageType, Integer.valueOf(nice));
        }
    }
    
    
    /**
     * Broadcasts a message to all observers
     * 
     * @param  message  The message to broadcast
     */
    public void broadcastMessage(final BlackboardMessage message)
    {
        synchronized (this.monitor)
        {
            System.err.println("BLACKBOARD.broadcastMessage(" + message.toString() + ")");
            final PriorityQueue<Integer> priorities = new PriorityQueue<Integer>();
            final HashMap<Integer, Vector<BlackboardObserver>> prioObservers = new HashMap<>();
            final HashSet<Integer> regdPrioes = new HashSet<Integer>();
            
            for (final BlackboardObserver observer : this.observers)
            {
                final HashMap<Class<? extends BlackboardMessage>, Integer> map = this.observationPriorities.get(observer);
                Integer priority = Integer.valueOf(0);
                if (map != null)
                {
                    Integer tmp;
                    if      ((tmp = map.get(message.getClass())) != null)  priority = tmp;
                    else if ((tmp = map.get(null)) != null)                priority = tmp;
                }
                if (regdPrioes.contains(priority) == false)
                {
                    priorities.add(priority);
                    regdPrioes.add(priority);
                }
                Vector<BlackboardObserver> vector = prioObservers.get(priority);
                if (vector == null)
                    prioObservers.put(priority, vector = new Vector<BlackboardObserver>());
                vector.add(observer);
            }
            
            for (Integer priority; (priority = priorities.poll()) != null;) // iterator messes up order
                for (final BlackboardObserver observer : prioObservers.get(priority))
                {
                    System.err.println("BLACKBOARD.broadcastMessage() ==> " + observer.toString());
                    final ThreadingPolicy policy;
                    final Runnable runnable = new Runnable()
                            {
                                /**
                                 * {@inheritDoc}
                                 */
                                @Override
                                public void run()
                                {   observer.messageBroadcasted(message);
                                }
                            };
                
                    final HashMap<Class<? extends BlackboardMessage>, ThreadingPolicy> map = this.observationThreading.get(observer);
                
                    if (map == null)
                        policy = null;
                    else if (map.containsKey(message.getClass()))
                        policy = map.get(message.getClass());
                    else
                        continue;
                
                    if (policy == null)  runnable.run();
                    else                 (new Thread(runnable)).start();
                }
            
            System.err.println("BLACKBOARD.broadcastMessage() <<<<");
        }
    }
    
}

