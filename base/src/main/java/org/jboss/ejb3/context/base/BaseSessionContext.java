/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ejb3.context.base;

import org.jboss.ejb3.context.CurrentInvocationContext;
import org.jboss.ejb3.context.spi.SessionBeanManager;
import org.jboss.ejb3.context.spi.SessionContext;
import org.jboss.ejb3.context.spi.SessionInvocationContext;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.xml.rpc.handler.MessageContext;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Implementation of the SessionContext interface.
 *
 * The class is Serializable to allow stateful beans to passivate.
 * 
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class BaseSessionContext extends BaseEJBContext
   implements Serializable, SessionContext
{
   private static final long serialVersionUID = 1L;

   /**
    * A serializable handle to be able to push this context into an object stream.
    */
   private static class Handle implements Serializable
   {
      private static final long serialVersionUID = 1L;

      private Object instance;

      private Handle(Object instance)
      {
         this.instance = instance;
      }

      private Object readResolve() throws ObjectStreamException
      {
         SessionBeanManager manager = CurrentInvocationContext.get(SessionInvocationContext.class).getManager();
         return new BaseSessionContext(manager, instance);
      }
   }

   public BaseSessionContext(SessionBeanManager manager, Object instance)
   {
      super(manager, instance);
   }

   public <T> T getBusinessObject(Class<T> businessInterface) throws IllegalStateException
   {
      // to allow override per invocation
      return getCurrentInvocationContext().getBusinessObject(businessInterface);
   }

   protected SessionInvocationContext getCurrentInvocationContext()
   {
      SessionInvocationContext current = CurrentInvocationContext.get(SessionInvocationContext.class);
      assert current.getEJBContext() == this;
      return current;
   }

   public EJBLocalObject getEJBLocalObject() throws IllegalStateException
   {
      // to allow override per invocation
      return getCurrentInvocationContext().getEJBLocalObject();
   }

   public EJBObject getEJBObject() throws IllegalStateException
   {
      // to allow override per invocation
      return getCurrentInvocationContext().getEJBObject();
   }

   public Class getInvokedBusinessInterface() throws IllegalStateException
   {
      final Class<?> businessInterface = getCurrentInvocationContext().getInvokedBusinessInterface();
      if (businessInterface == null)
      {
         throw new IllegalStateException("Call to " + SessionContext.class.getName()
               + ".getInvokedBusinessInterface() was made from outside an EJB3 Business Interface "
               + "(possibly an EJB2.x Remote/Local?). " + "EJB 3.0 Specification 4.5.2.");
      }
      return businessInterface;
   }

   public SessionBeanManager getManager()
   {
      return (SessionBeanManager) super.getManager();
   }
   
   public MessageContext getMessageContext() throws IllegalStateException
   {
      return getCurrentInvocationContext().getMessageContext();
   }

   public boolean wasCancelCalled() throws IllegalStateException
   {
      return getCurrentInvocationContext().wasCancelCalled();
   }

   private Object writeReplace() throws ObjectStreamException
   {
      // the manager is never Serializable, so we should only make sure the bean ends up in the stream.
      // note that the bean itself doesn't have to be Serializable, but the assumption is that a capable stream
      // is used.
      return new Handle(getTarget());
   }
}
