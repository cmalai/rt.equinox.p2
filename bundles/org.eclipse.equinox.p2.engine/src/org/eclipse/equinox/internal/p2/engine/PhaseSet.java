/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.phases.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class PhaseSet implements IPhaseSet {

	public static final List DEFAULT_PHASES = Arrays.asList(new String[] {IPhaseSet.PHASE_COLLECT, IPhaseSet.PHASE_UNCONFIGURE, IPhaseSet.PHASE_UNINSTALL, IPhaseSet.PHASE_PROPERTY, IPhaseSet.PHASE_CHECK_TRUST, IPhaseSet.PHASE_INSTALL, IPhaseSet.PHASE_CONFIGURE});

	public static final boolean forcedUninstall = Boolean.valueOf(EngineActivator.getContext().getProperty("org.eclipse.equinox.p2.engine.forcedUninstall")).booleanValue(); //$NON-NLS-1$

	private final Phase[] phases;

	public static IPhaseSet createPhaseSetExcluding(String[] excludes) {
		ArrayList phases = new ArrayList(DEFAULT_PHASES);
		if (excludes != null) {
			for (int i = 0; i < excludes.length; i++) {
				phases.remove(excludes[i]);
			}
		}
		return createPhaseSetIncluding((String[]) phases.toArray(new String[phases.size()]));
	}

	public static IPhaseSet createPhaseSetIncluding(String[] includes) {
		ArrayList phases = new ArrayList();
		for (int i = 0; i < includes.length; i++) {
			String current = includes[i];
			if (current.equals(IPhaseSet.PHASE_CONFIGURE))
				phases.add(new Configure(10));
			else if (current.equals(IPhaseSet.PHASE_CHECK_TRUST))
				phases.add(new CheckTrust(10));
			else if (current.equals(IPhaseSet.PHASE_COLLECT))
				phases.add(new Collect(100));
			else if (current.equals(IPhaseSet.PHASE_INSTALL))
				phases.add(new Install(50));
			else if (current.equals(IPhaseSet.PHASE_PROPERTY))
				phases.add(new Property(1));
			else if (current.equals(IPhaseSet.PHASE_UNCONFIGURE))
				phases.add(new Unconfigure(10, forcedUninstall));
			else if (current.equals(IPhaseSet.PHASE_UNINSTALL))
				phases.add(new Uninstall(50, forcedUninstall));
		}
		return new PhaseSet((Phase[]) phases.toArray(new Phase[phases.size()]));
	}

	public PhaseSet(Phase[] phases) {
		if (phases == null)
			throw new IllegalArgumentException(Messages.null_phases);

		this.phases = phases;
	}

	public final MultiStatus perform(EngineSession session, Operand[] operands, IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		int[] weights = getProgressWeights(operands);
		int totalWork = getTotalWork(weights);
		SubMonitor pm = SubMonitor.convert(monitor, totalWork);
		try {
			for (int i = 0; i < phases.length; i++) {
				if (pm.isCanceled()) {
					status.add(Status.CANCEL_STATUS);
					return status;
				}
				Phase phase = phases[i];
				phase.actionManager = (ActionManager) session.getAgent().getService(ActionManager.SERVICE_NAME);
				try {
					phase.perform(status, session, operands, pm.newChild(weights[i]));
				} catch (OperationCanceledException e) {
					// propagate operation cancellation
					status.add(new Status(IStatus.CANCEL, EngineActivator.ID, e.getMessage(), e));
				} catch (RuntimeException e) {
					// "perform" calls user code and might throw an unchecked exception
					// we catch the error here to gather information on where the problem occurred.
					status.add(new Status(IStatus.ERROR, EngineActivator.ID, e.getMessage(), e));
				} catch (LinkageError e) {
					// Catch linkage errors as these are generally recoverable but let other Errors propagate (see bug 222001)
					status.add(new Status(IStatus.ERROR, EngineActivator.ID, e.getMessage(), e));
				} finally {
					phase.actionManager = null;
				}
				if (status.matches(IStatus.CANCEL)) {
					MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.CANCEL, Messages.Engine_Operation_Canceled_By_User, null);
					result.merge(status);
					return result;
				} else if (status.matches(IStatus.ERROR)) {
					MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.ERROR, phase.getProblemMessage(), null);
					result.add(new Status(IStatus.ERROR, EngineActivator.ID, session.getContextString(), null));
					result.merge(status);
					return result;
				}
			}
		} finally {
			pm.done();
		}
		return status;
	}

	public final IStatus validate(ActionManager actionManager, IProfile profile, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {
		Set missingActions = new HashSet();
		for (int i = 0; i < phases.length; i++) {
			Phase phase = phases[i];
			phase.actionManager = actionManager;
			try {
				for (int j = 0; j < operands.length; j++) {
					Operand operand = operands[j];
					try {
						if (!phase.isApplicable(operand))
							continue;

						ProvisioningAction[] actions = phase.getActions(operand);
						if (actions == null)
							continue;
						for (int k = 0; k < actions.length; k++) {
							ProvisioningAction action = actions[k];
							if (action instanceof MissingAction)
								missingActions.add(action);
						}
					} catch (RuntimeException e) {
						// "perform" calls user code and might throw an unchecked exception
						// we catch the error here to gather information on where the problem occurred.
						return new Status(IStatus.ERROR, EngineActivator.ID, e.getMessage() + " " + getContextString(profile, phase, operand), e); //$NON-NLS-1$
					} catch (LinkageError e) {
						// Catch linkage errors as these are generally recoverable but let other Errors propagate (see bug 222001)
						return new Status(IStatus.ERROR, EngineActivator.ID, e.getMessage() + " " + getContextString(profile, phase, operand), e); //$NON-NLS-1$
					}
				}
			} finally {
				phase.actionManager = null;
			}
		}
		if (!missingActions.isEmpty()) {
			MissingAction[] missingActionsArray = (MissingAction[]) missingActions.toArray(new MissingAction[missingActions.size()]);
			MissingActionsException exception = new MissingActionsException(missingActionsArray);
			return (new Status(IStatus.ERROR, EngineActivator.ID, exception.getMessage(), exception));
		}
		return Status.OK_STATUS;
	}

	private String getContextString(IProfile profile, Phase phase, Operand operand) {
		return NLS.bind(Messages.session_context, new Object[] {profile.getProfileId(), phase.getClass().getName(), operand.toString(), ""}); //$NON-NLS-1$
	}

	private int getTotalWork(int[] weights) {
		int sum = 0;
		for (int i = 0; i < weights.length; i++)
			sum += weights[i];
		return sum;
	}

	private int[] getProgressWeights(Operand[] operands) {
		int[] weights = new int[phases.length];
		for (int i = 0; i < phases.length; i += 1) {
			if (operands.length > 0)
				//alter weights according to the number of operands applicable to that phase
				weights[i] = (phases[i].weight * countApplicable(phases[i], operands) / operands.length);
			else
				weights[i] = phases[i].weight;
		}
		return weights;
	}

	private int countApplicable(Phase phase, Operand[] operands) {
		int count = 0;
		for (int i = 0; i < operands.length; i++) {
			if (phase.isApplicable(operands[i]))
				count++;
		}
		return count;
	}

	public String[] getPhaseIds() {
		String[] ids = new String[phases.length];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = phases[i].phaseId;
		}
		return ids;
	}

	public Phase[] getPhases() {
		return phases;
	}
}