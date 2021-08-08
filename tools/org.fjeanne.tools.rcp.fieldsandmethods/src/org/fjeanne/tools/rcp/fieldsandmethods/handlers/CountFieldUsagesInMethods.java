package org.fjeanne.tools.rcp.fieldsandmethods.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.ASTVisitor;
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
		System.out.println("Declared field: '" + node.getName() + "'");
		_fieldsToMethods.put(node.getName().getFullyQualifiedName(), new ArrayList<>());
		return false;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		_currentMethod = node.getName().getFullyQualifiedName();
		System.out.println("Entering method: " + _currentMethod);
		return true;
	}

	@Override
	public boolean visit(SimpleName node) {
		String name = node.getFullyQualifiedName();
		if (_fieldsToMethods.containsKey(name)) {
			System.out.println("Field '" + name + "' is used in method '" + _currentMethod + "'");
			_fieldsToMethods.get(name).add(_currentMethod);
		}
		return false;
	}

}
