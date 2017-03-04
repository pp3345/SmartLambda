package edu.teco.smartlambda.lambda;

import com.google.common.util.concurrent.ListenableFuture;
import edu.teco.smartlambda.monitoring.MonitoringEvent;
import edu.teco.smartlambda.schedule.Event;
import edu.teco.smartlambda.shared.ExecutionReturnValue;

import java.util.List;
import java.util.Optional;

/**
 * Decorates lambdas with authenticagtion and aborts the lambda call, if authentication fails
 */
//// FIXME: 2/15/17 
public class PermissionDecorator extends LambdaDecorator {
	
	public PermissionDecorator(final AbstractLambda lambda) {
		super(lambda);
	}
	
	@Override
	public Optional<ExecutionReturnValue> executeSync(final String params) {
		return super.executeSync(params);
	}
	
	@Override
	public ListenableFuture<ExecutionReturnValue> executeAsync(final String params) { return super.executeAsync(params);}
	
	@Override
	public void save() {
		super.save();
	}
	
	@Override
	public void update() {
		super.update();
	}
	
	@Override
	public void delete() {
		super.delete();
	}
	
	@Override
	public void schedule(final Event event) {
		super.schedule(event);
	}
	
	@Override
	public void deployBinary(final byte[] content) {
		super.deployBinary(content);
	}
	
	@Override
	public Event getScheduledEvent(final String name) {
		return super.getScheduledEvent(name);
	}
	
	@Override
	public List<Event> getScheduledEvents() {
		return super.getScheduledEvents();
	}
	
	@Override
	public List<MonitoringEvent> getMonitoringEvents() {
		return super.getMonitoringEvents();
	}
}
