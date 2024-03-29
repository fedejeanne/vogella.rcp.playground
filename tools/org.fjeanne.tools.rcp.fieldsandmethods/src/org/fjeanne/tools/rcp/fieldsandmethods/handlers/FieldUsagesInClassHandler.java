package org.fjeanne.tools.rcp.fieldsandmethods.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class FieldUsagesInClassHandler extends AbstractHandler {

	public static final String PLUGIN_ID = "org.fjeanne.tools.rcp.fieldsandmethods";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		toStringOfSelectedItems(event);

		List<Object> selectedItems = getSelectedItems(event);

		Collection<ITypeRoot> compilationUnits = selectedItems.stream()//
				.filter(ITypeRoot.class::isInstance)//
				.map(ITypeRoot.class::cast)//
				.collect(Collectors.toList());

		if (compilationUnits.isEmpty()) {
			showWarningWrongInput(event, compilationUnits);
			return null;
		}

		String result = findFieldUsagesInMethods(compilationUnits);
		showResult(event, result);

		return null;
	}

	private String findFieldUsagesInMethods(Collection<ITypeRoot> compilationUnits) {
		StringBuilder result = new StringBuilder();

		compilationUnits.forEach(compilationUnit -> {
			result.append(compilationUnit.getElementName())//
					.append(System.lineSeparator());

			Map<String, List<String>> findFieldUsagesInMethods = findFieldUsagesInMethods(compilationUnit);

			Optional<String> csvReport = toCsv(findFieldUsagesInMethods);
			result.append(csvReport.orElse("No fields found"))//
					.append(System.lineSeparator());

		});
		return result.toString();
	}

	private Optional<String> toCsv(Map<String, List<String>> fieldToMethods) {
		if (fieldToMethods.isEmpty())
			return Optional.empty();

		StringBuilder sb = new StringBuilder();

		Set<String> methods = getAllMethodNamesOrdered(fieldToMethods);

		String header = "field," + methods.stream().collect(Collectors.joining(",")) + ",total";
		sb.append(header);
		sb.append(System.lineSeparator());

		for (String field : fieldToMethods.keySet()) {
			sb.append(field);
			sb.append(",");

			List<String> usagesOfFieldInMethods = fieldToMethods.get(field);
			List<Long> useCountInMethods = collectUseCountInMethods(usagesOfFieldInMethods, methods);

			String useCountString = useCountInMethods.stream().map(String::valueOf).collect(Collectors.joining(","));
			sb.append(useCountString);

			sb.append(",");
			long total = useCountInMethods.parallelStream().reduce(0L, Long::sum);
			sb.append(total);

			sb.append(System.lineSeparator());
		}

		return Optional.of(sb.toString());
	}

	private List<Long> collectUseCountInMethods(List<String> methodUsages, Set<String> allMethods) {
		List<Long> useCountInMethods = new ArrayList<>();

		for (String m : allMethods) {
			long count = countOccurrencesInList(m, methodUsages);

			useCountInMethods.add(count);
		}
		return useCountInMethods;
	}

	private long countOccurrencesInList(String s, List<String> list) {
		return list.stream().filter(s::equals).count();
	}

	private Set<String> getAllMethodNamesOrdered(Map<String, List<String>> fieldToMethods) {
		return fieldToMethods.values().parallelStream()//
				.flatMap(Collection::stream)//
				.filter(c -> c != null) //
				.map(c -> c.toString()) //
				.collect(Collectors.toCollection(TreeSet::new));
	}

	private void showWarningWrongInput(ExecutionEvent event, Collection<ITypeRoot> compilationUnits)
			throws ExecutionException {

		showWarning(event, "To be used only on Java files", //
				"This command only works on java files and not on packages, projects, etc.\n"//
						+ "Please select a Java file in the package explorer and run the command again.");
	}

	private void showResult(ExecutionEvent event, String report) throws ExecutionException {
		if (report.isBlank()) {
			showWarning(event, "No fields found", "I couldn't find any fields in the selected class(es)");
			return;
		}

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		IStatus status = createMultiStatus(report);
		ErrorDialog.openError(window.getShell(), "Field usages in methods", "You can check the result under 'Details'."//
				+ System.lineSeparator()//
				+"HINT: you can copy the result and paste it in Excel, it's in CSV format.", status);
	}

	private void showWarning(ExecutionEvent event, String title, String message) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openWarning(window.getShell(), title, message);
	}

	private static MultiStatus createMultiStatus(String details) {
		Status status = new Status(IStatus.INFO, PLUGIN_ID, details);

		return new MultiStatus(PLUGIN_ID, IStatus.INFO, List.of(status).toArray(new Status[] {}), null, null);
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
