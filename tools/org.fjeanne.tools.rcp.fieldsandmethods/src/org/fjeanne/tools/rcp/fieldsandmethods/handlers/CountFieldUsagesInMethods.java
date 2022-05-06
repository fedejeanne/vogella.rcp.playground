package org.fjeanne.tools.rcp.fieldsandmethods.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class CountFieldUsagesInMethods extends ASTVisitor {

	private final Map<String, List<String>> _fieldsToMethods = new TreeMap<>();

	private String _currentMethod;

	public Map<String, List<String>> getFieldsToMethods() {
		return _fieldsToMethods;
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		String name = node.getName().getFullyQualifiedName();

		if (!isField(node)) {
			System.out.println("'" + name + "' is not a field. SKIP");
			return true;
		}

		System.out.println("Declared field: '" + name + "'");
		_fieldsToMethods.put(name, new ArrayList<>());
		return true;
	}

	private boolean isField(VariableDeclarationFragment node) {
		return node.getParent() instanceof FieldDeclaration;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		_currentMethod = node.getName().getFullyQualifiedName();
		System.out.println("Entering method: " + _currentMethod);
		return true;
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		System.out.println("Leaving method: " + _currentMethod);
		_currentMethod = null;
	}

	@Override
	public boolean visit(SimpleName node) {
		String name = node.getFullyQualifiedName();
		System.out.print("Inspecting possible field: '" + name + "'");
		if (_fieldsToMethods.containsKey(name)) {
			System.out.println("Field '" + name + "' is used in method '" + _currentMethod + "'");
			_fieldsToMethods.get(name).add(_currentMethod);
		} else
			System.out.println("... this isn't a field");

		return true;
	}

}
