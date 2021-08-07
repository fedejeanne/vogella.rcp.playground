package de.vogella.rcp.commands.first.handlers;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class SampleHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		return showPopup(event);
	}

	private Object showPopup(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(window.getShell(), "You selected...", toStringOfSelectedItems(event));
		return null;
	}

	private String toStringOfSelectedItems(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();

		if (selection != null & selection instanceof IStructuredSelection) {
			StringBuilder sb = new StringBuilder();

			IStructuredSelection strucSelection = (IStructuredSelection) selection;

			int i = 1;

			for (Iterator<Object> iterator = strucSelection.iterator(); iterator.hasNext();) {
				Object element = iterator.next();
				sb.append("Element nr. ").append(i).append(": ")//
						.append("(").append(element.getClass()).append(") ")//
						.append(element.toString())//
						.append(System.lineSeparator())//
						.append(System.lineSeparator());
				++i;
			}

			return sb.toString();
		}
		return null;
	}

}
