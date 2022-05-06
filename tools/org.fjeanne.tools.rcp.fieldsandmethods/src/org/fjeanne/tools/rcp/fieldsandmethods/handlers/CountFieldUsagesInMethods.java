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
			log("'" + name + "' is not a field. SKIP");
			return true;
		}

		log("Declared field: '" + name + "'");
		_fieldsToMethods.put(name, new ArrayList<>());
		return true;
	}

	private boolean isField(VariableDeclarationFragment node) {
		return node.getParent() instanceof FieldDeclaration;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		_currentMethod = node.getName().getFullyQualifiedName();
		log("Entering method: " + _currentMethod);
		return true;
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		log("Leaving method: " + _currentMethod);
		_currentMethod = null;
	}

	@Override
	public boolean visit(SimpleName node) {
		String name = node.getFullyQualifiedName();
		log("Inspecting possible field: '" + name + "'");
		if (_fieldsToMethods.containsKey(name)) {
			log("Field '" + name + "' is used in method '" + _currentMethod + "'");
			_fieldsToMethods.get(name).add(_currentMethod);
		} else
			log("... this isn't a field");

		return true;
	}

	private void log(String message) {
		//TODO use a proper logging mechanism
		System.out.println(message);
	}

}
