package org.fjeanne.tools.rcp.fieldsandmethods.handlers;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class FieldUsagesInClassHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		toStringOfSelectedItems(event);

		List<Object> selectedItems = getSelectedItems(event);

		Stream<ITypeRoot> compilationUnits = selectedItems.parallelStream()//
				.filter(ITypeRoot.class::isInstance)//
				.map(ITypeRoot.class::cast);

		String result = findFieldUsagesInMethods(compilationUnits);

		showResultIfNotEmpty(event, result);

		return null;
	}

	private String findFieldUsagesInMethods(Stream<ITypeRoot> compilationUnits) {
		StringBuilder result = new StringBuilder();
		
		compilationUnits.forEach(compilationUnit -> {
			result.append(compilationUnit.getElementName())//
					.append(System.lineSeparator());

			Map<String, List<String>> findFieldUsagesInMethods = findFieldUsagesInMethods(compilationUnit);
			result.append(toString(findFieldUsagesInMethods))//
					.append(System.lineSeparator());

		});
		return result.toString();
	}

	private void showResultIfNotEmpty(ExecutionEvent event, String report) throws ExecutionException {
		if (report.isBlank())
			return;

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(window.getShell(), "Field usage in methods", report.toString());
	}

	private String toString(Map<String, List<String>> fieldsToMethods) {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, List<String>> fieldToMethods : fieldsToMethods.entrySet()) {
			String fieldName = fieldToMethods.getKey();
			List<String> methods = fieldToMethods.getValue();

			Collections.sort(methods);
			String methodNames = methods.stream().collect(Collectors.joining(",", "[", "]"));

			sb.append("'");
			sb.append(fieldName);
			sb.append("' is used ");
			sb.append(methods.size());
			sb.append(" times in these methods: ");
			sb.append(methodNames);
			sb.append(System.lineSeparator());
			sb.append(System.lineSeparator());

		}

		return sb.toString();
	}

	private Map<String, List<String>> findFieldUsagesInMethods(ITypeRoot compilationUnit) {
		CountFieldUsagesInMethods countFieldUsagesVisitor = new CountFieldUsagesInMethods();
		ASTParser parser = ASTParser.newParser(AST.JLS11);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(compilationUnit);

		ASTNode node = parser.createAST(new NullProgressMonitor());
		node.accept(countFieldUsagesVisitor);

		Map<String, List<String>> fieldsToMethods = countFieldUsagesVisitor.getFieldsToMethods();
		return fieldsToMethods;
	}

	private String toStringOfSelectedItems(ExecutionEvent event) {

		List<Object> selectedItems = getSelectedItems(event);

		return toStringOfItems(selectedItems);
	}

	private String toStringOfItems(List<Object> selectedItems) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < selectedItems.size(); ++i) {
			Object element = selectedItems.get(i);

			sb.append("Element nr. ").append(i).append(": ")//
					.append("(").append(element.getClass()).append(") ")//
					.append(element.toString())//
					.append(System.lineSeparator())//
					.append(System.lineSeparator());
		}

		return sb.toString();
	}

	private List<Object> getSelectedItems(ExecutionEvent event) {
		List<Object> selectedItems = new LinkedList<>();
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();

		if (selection != null & selection instanceof IStructuredSelection) {

			IStructuredSelection strucSelection = (IStructuredSelection) selection;

			for (Iterator<Object> iterator = strucSelection.iterator(); iterator.hasNext();) {
				Object element = iterator.next();
				selectedItems.add(element);
			}

		}
		return selectedItems;
	}
}
