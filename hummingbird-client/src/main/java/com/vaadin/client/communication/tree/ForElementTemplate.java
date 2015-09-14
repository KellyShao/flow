package com.vaadin.client.communication.tree;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.communication.tree.EventArray.ArrayEventListener;

import elemental.json.JsonObject;

public class ForElementTemplate extends Template {
    private final TreeUpdater treeUpdater;
    private final Template childTemplate;
    private final String modelKey;
    private final String innerScope;

    public ForElementTemplate(TreeUpdater treeUpdater,
            JsonObject templateDescription, int templateId) {
        super(templateId);
        this.treeUpdater = treeUpdater;
        modelKey = templateDescription.getString("modelKey");
        innerScope = templateDescription.getString("innerScope");

        childTemplate = new BoundElementTemplate(treeUpdater,
                templateDescription, templateId);
    }

    @Override
    public Node createElement(TreeNode node, NodeContext outerContext) {
        // Creates anchor element
        Node commentNode = createCommentNode("for " + modelKey);

        outerContext.resolveArrayProperty(getModelKey())
                .addArrayEventListener(new ArrayEventListener() {

                    @Override
                    public void splice(EventArray eventArray, int startIndex,
                            JsArrayObject<Object> removed,
                            JsArrayObject<Object> added) {
                        // TODO Reuse reference node if processing multiple
                        // items
                        for (int i = 0; i < removed.size(); i++) {
                            Node node = findNodeBefore(commentNode, startIndex)
                                    .getNextSibling();

                            node.removeFromParent();
                        }

                        for (int i = 0; i < added.size(); i++) {
                            TreeNode childNode = (TreeNode) added.get(i);

                            NodeContext innerContext = new NodeContext() {
                                @Override
                                public EventArray resolveArrayProperty(
                                        String name) {
                                    if (name.startsWith(getInnerScope())) {
                                        return childNode.getArrayProperty(
                                                name.substring(getInnerScope()
                                                        .length()));
                                    } else {
                                        return outerContext
                                                .resolveArrayProperty(name);
                                    }
                                }

                                @Override
                                public TreeNodeProperty resolveProperty(
                                        String name) {
                                    if (name.startsWith(getInnerScope())) {
                                        String innerProperty = name.substring(
                                                getInnerScope().length() + 1);
                                        return childNode
                                                .getProperty(innerProperty);
                                    } else {
                                        return outerContext
                                                .resolveProperty(name);
                                    }
                                }

                                @Override
                                public Map<String, JavaScriptObject> buildEventHandlerContext() {
                                    Map<String, JavaScriptObject> contextMap = outerContext
                                            .buildEventHandlerContext();
                                    contextMap.put(getInnerScope(),
                                            childNode.getProxy());
                                    return contextMap;
                                }
                            };

                            Node child = treeUpdater.createElement(
                                    getChildTemplate(), childNode,
                                    innerContext);

                            Node insertionPoint = findNodeBefore(commentNode,
                                    startIndex + i);

                            insertionPoint.getParentElement().insertAfter(child,
                                    insertionPoint);
                        }
                    }
                });

        return commentNode;
    }

    private static Node findNodeBefore(Node anchorNode, int index) {
        Node refChild = anchorNode;
        for (int i = 0; i < index; i++) {
            refChild = refChild.getNextSibling();
        }
        return refChild;
    }

    private static native Node createCommentNode(String comment)
    /*-{
        return $doc.createComment(comment);
    }-*/;

    private String getModelKey() {
        return modelKey;
    }

    private String getInnerScope() {
        return innerScope;
    }

    private Template getChildTemplate() {
        return childTemplate;
    }
}