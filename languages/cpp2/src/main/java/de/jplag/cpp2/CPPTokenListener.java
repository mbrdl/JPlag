package de.jplag.cpp2;

import de.jplag.semantics.VariableHelper;
import org.antlr.v4.runtime.ParserRuleContext;

import de.jplag.cpp2.grammar.CPP14Parser;
import de.jplag.cpp2.grammar.CPP14ParserBaseListener;

import java.util.Set;

public class CPPTokenListener extends CPP14ParserBaseListener {

    private final Parser parser;
    private final VariableHelper variableHelper;

    public CPPTokenListener(Parser parser) {
        this.parser = parser;
        this.variableHelper = new VariableHelper();
    }

    @Override
    public void enterCompoundStatement(CPP14Parser.CompoundStatementContext ctx) {
        variableHelper.enterLocalScope();
        parser.addEnter(CPPTokenType.C_BLOCK_BEGIN, ctx.getStart());
    }

    @Override
    public void exitCompoundStatement(CPP14Parser.CompoundStatementContext ctx) {
        variableHelper.exitLocalScope();
        parser.addExit(CPPTokenType.C_BLOCK_END, ctx.getStop());
    }

    @Override
    public void enterUnqualifiedId(CPP14Parser.UnqualifiedIdContext ctx) {
        // assumption: all local variable references are unqualified
        // may or may not be correct but good enough heuristic anyways
        var parentCtx = ctx.getParent().getParent();
        if (!parentCtx.getParent().getParent().getText().contains("(")) {
            boolean register = true;
            boolean afterDot = parentCtx.getClass() == CPP14Parser.PostfixExpressionContext.class;
            if (afterDot) {
                // bad approximation but I don't care at this point
                register = ((CPP14Parser.PostfixExpressionContext) parentCtx).postfixExpression().getText().equals("this");
            }
            if (register)
                System.out.println("register variable use " + ctx.getText());
        }
    }

    @Override
    public void enterClassSpecifier(CPP14Parser.ClassSpecifierContext ctx) {
        for (CPP14Parser.MemberdeclarationContext member : ctx.memberSpecification().memberdeclaration()) {
            if (member.memberDeclaratorList() != null) {
                // I don't even know man
                CPP14Parser.SimpleTypeSpecifierContext ugh = member.declSpecifierSeq().declSpecifier().get(0).typeSpecifier().trailingTypeSpecifier().simpleTypeSpecifier();
                boolean typeMutable = ugh.theTypeName() != null;
                for (var decl : member.memberDeclaratorList().memberDeclarator()) {
                    // decl.declarator().noPointerDeclarator() may alternatively be used, todo
                    CPP14Parser.PointerDeclaratorContext pd = decl.declarator().pointerDeclarator();
                    boolean mutable = typeMutable || !pd.pointerOperator().isEmpty();
                    String name = pd.noPointerDeclarator().getText();
                    System.out.print("register member variable " + name);
                    System.out.println(mutable ? " (mutable)" : "");
                    variableHelper.registerMemberVariable(name, mutable);
                }
            }
        }
        CPP14Parser.ClassKeyContext classKey = ctx.classHead().classKey();
        if (classKey.Class() != null) {
            parser.addEnter(CPPTokenType.C_CLASS_BEGIN, ctx.getStart());
        } else if (classKey.Struct() != null) {
            parser.addEnter(CPPTokenType.C_STRUCT_BEGIN, ctx.getStart());
        }
    }

    @Override
    public void exitClassSpecifier(CPP14Parser.ClassSpecifierContext ctx) {
        CPP14Parser.ClassKeyContext classKey = ctx.classHead().classKey();
        if (classKey.Class() != null) {
            parser.addExit(CPPTokenType.C_CLASS_END, ctx.getStop());
        } else if (classKey.Struct() != null) {
            parser.addExit(CPPTokenType.C_STRUCT_END, ctx.getStop());
        }
        variableHelper.clearMemberVariables();
    }

    @Override
    public void enterEnumSpecifier(CPP14Parser.EnumSpecifierContext ctx) {
        parser.addEnter(CPPTokenType.C_ENUM_BEGIN, ctx.getStart());
    }

    @Override
    public void exitEnumSpecifier(CPP14Parser.EnumSpecifierContext ctx) {
        parser.addExit(CPPTokenType.C_ENUM_END, ctx.getStop());
    }

    @Override
    public void enterFunctionDefinition(CPP14Parser.FunctionDefinitionContext ctx) {
        variableHelper.enterLocalScope();
        parser.addEnter(CPPTokenType.C_FUNCTION_BEGIN, ctx.getStart());
    }

    @Override
    public void exitFunctionDefinition(CPP14Parser.FunctionDefinitionContext ctx) {
        variableHelper.exitLocalScope();
        parser.addExit(CPPTokenType.C_FUNCTION_END, ctx.getStop());
    }

    @Override
    public void enterIterationStatement(CPP14Parser.IterationStatementContext ctx) {
        if (ctx.Do() != null) {
            parser.addEnter(CPPTokenType.C_DO_BEGIN, ctx.getStart());
        } else if (ctx.For() != null) {
            variableHelper.enterLocalScope();
            parser.addEnter(CPPTokenType.C_FOR_BEGIN, ctx.getStart());
        } else if (ctx.While() != null) {
            parser.addEnter(CPPTokenType.C_WHILE_BEGIN, ctx.getStart());
        }
    }

    @Override
    public void exitIterationStatement(CPP14Parser.IterationStatementContext ctx) {
        if (ctx.Do() != null) {
            parser.addEnter(CPPTokenType.C_DO_END, ctx.getStop());
        } else if (ctx.For() != null) {
            variableHelper.exitLocalScope();
            parser.addEnter(CPPTokenType.C_FOR_END, ctx.getStop());
        } else if (ctx.While() != null) {
            parser.addEnter(CPPTokenType.C_WHILE_END, ctx.getStop());
        }
    }

    @Override
    public void enterSelectionStatement(CPP14Parser.SelectionStatementContext ctx) {
        if (ctx.Switch() != null) {
            parser.addEnter(CPPTokenType.C_SWITCH_BEGIN, ctx.getStart());
        } else if (ctx.If() != null) {
            parser.addEnter(CPPTokenType.C_IF_BEGIN, ctx.getStart());
            if (ctx.Else() != null) {
                parser.addEnter(CPPTokenType.C_ELSE, ctx.Else().getSymbol());
            }
        }
    }

    @Override
    public void exitSelectionStatement(CPP14Parser.SelectionStatementContext ctx) {
        if (ctx.Switch() != null) {
            parser.addEnter(CPPTokenType.C_SWITCH_END, ctx.getStop());
        } else if (ctx.If() != null) {
            parser.addEnter(CPPTokenType.C_IF_END, ctx.getStop());
        }
    }

    @Override
    public void enterLabeledStatement(CPP14Parser.LabeledStatementContext ctx) {
        if (ctx.Case() != null) {
            parser.addEnter(CPPTokenType.C_CASE, ctx.getStart());
        } else if (ctx.Default() != null) {
            parser.addEnter(CPPTokenType.C_DEFAULT, ctx.getStart());
        }
    }

    @Override
    public void enterTryBlock(CPP14Parser.TryBlockContext ctx) {
        parser.addEnter(CPPTokenType.C_TRY, ctx.getStart());
    }

    @Override
    public void enterHandler(CPP14Parser.HandlerContext ctx) {
        variableHelper.enterLocalScope();
        parser.addEnter(CPPTokenType.C_CATCH_BEGIN, ctx.getStart());
    }

    @Override
    public void exitHandler(CPP14Parser.HandlerContext ctx) {
        variableHelper.exitLocalScope();
        parser.addEnter(CPPTokenType.C_CATCH_END, ctx.getStop());
    }

    @Override
    public void enterJumpStatement(CPP14Parser.JumpStatementContext ctx) {
        if (ctx.Break() != null) {
            parser.addEnter(CPPTokenType.C_BREAK, ctx.getStart());
        } else if (ctx.Continue() != null) {
            parser.addEnter(CPPTokenType.C_CONTINUE, ctx.getStart());
        } else if (ctx.Goto() != null) {
            parser.addEnter(CPPTokenType.C_GOTO, ctx.getStart());
        } else if (ctx.Return() != null) {
            parser.addEnter(CPPTokenType.C_RETURN, ctx.getStart());
        }
    }

    @Override
    public void enterThrowExpression(CPP14Parser.ThrowExpressionContext ctx) {
        parser.addEnter(CPPTokenType.C_THROW, ctx.getStart());
    }

    @Override
    public void enterNewExpression(CPP14Parser.NewExpressionContext ctx) {
        // TODO NEWARRAY, ARRAYINIT
        if (ctx.newInitializer() == null) {
            parser.addEnter(CPPTokenType.C_NEWARRAY, ctx.getStart());
        } else {
            parser.addEnter(CPPTokenType.C_NEWCLASS, ctx.getStart());
        }
    }

    @Override
    public void enterTemplateDeclaration(CPP14Parser.TemplateDeclarationContext ctx) {
        parser.addEnter(CPPTokenType.C_GENERIC, ctx.getStart());
    }

    @Override
    public void enterAssignmentOperator(CPP14Parser.AssignmentOperatorContext ctx) {
        // does not cover ++, --, this is done via UnaryExpressionContext and PostfixExpressionContext
        // does not cover all =, this is done via BraceOrEqualInitializerContext
        parser.addEnter(CPPTokenType.C_ASSIGN, ctx.getStart());
    }

    @Override
    public void enterBraceOrEqualInitializer(CPP14Parser.BraceOrEqualInitializerContext ctx) {
        if (ctx.Assign() != null) {
            parser.addEnter(CPPTokenType.C_ASSIGN, ctx.getStart());
        }
    }

    @Override
    public void enterUnaryExpression(CPP14Parser.UnaryExpressionContext ctx) {
        if (ctx.PlusPlus() != null || ctx.MinusMinus() != null) {
            parser.addEnter(CPPTokenType.C_ASSIGN, ctx.getStart());
        }
    }

    @Override
    public void enterStaticAssertDeclaration(CPP14Parser.StaticAssertDeclarationContext ctx) {
        parser.addEnter(CPPTokenType.C_STATIC_ASSERT, ctx.getStart());
    }

    @Override
    public void enterEnumeratorDefinition(CPP14Parser.EnumeratorDefinitionContext ctx) {
        parser.addEnter(CPPTokenType.C_VARDEF, ctx.getStart());
    }

    @Override
    public void enterSimpleTypeSpecifier(CPP14Parser.SimpleTypeSpecifierContext ctx) {
        if (hasIndirectParent(ctx, CPP14Parser.MemberdeclarationContext.class, CPP14Parser.FunctionDefinitionContext.class)) {
            parser.addEnter(CPPTokenType.C_VARDEF, ctx.getStart()); // member variable
        } else { // local variable
            CPP14Parser.SimpleDeclarationContext parent = getIndirectParent(ctx, CPP14Parser.SimpleDeclarationContext.class, CPP14Parser.TemplateArgumentContext.class, CPP14Parser.FunctionDefinitionContext.class);
            if (parent != null) {
                // part of a SimpleDeclaration without being part of
                //  - a TemplateArgument (vector<HERE> v)
                //  - a FunctionDefinition (return type, parameters)
                //  first.
                if (parent.getText().contains("(")) { // TODO do not depend on text
                    // method calls like A::b()
                    parser.addEnter(CPPTokenType.C_APPLY, parent.getStart());
                } else if (!hasIndirectParent(ctx, CPP14Parser.NewTypeIdContext.class)) {
                    // 'new <Type>' does not declare a new variable
                    boolean typeMutable = ctx.theTypeName() != null; // block is duplicate to member variable register
                    for (var decl : parent.initDeclaratorList().initDeclarator()) {
                        String name = decl.declarator().getText();
                        CPP14Parser.PointerDeclaratorContext pd = decl.declarator().pointerDeclarator();
                        boolean mutable = typeMutable || !pd.pointerOperator().isEmpty();
                        System.out.print("register local variable " + name);
                        System.out.println(mutable ? " (mutable)" : "");
                        variableHelper.registerLocalVariable(name, mutable);
                    }
                    parser.addEnter(CPPTokenType.C_VARDEF, ctx.getStart());
                }
            }
        }
    }

    @Override
    public void enterConditionalExpression(CPP14Parser.ConditionalExpressionContext ctx) {
        if (ctx.Question() != null) {
            parser.addEnter(CPPTokenType.C_QUESTIONMARK, ctx.getStart());
        }
    }

    @Override
    public void enterPostfixExpression(CPP14Parser.PostfixExpressionContext ctx) {
        // TODO this only covers foo->bar() and foo.bar()
        // Foo::bar() is handled in SimpleTypeSpecifierContext
        if (ctx.LeftParen() != null) {
            parser.addEnter(CPPTokenType.C_APPLY, ctx.getStart());
        } else if (ctx.PlusPlus() != null || ctx.MinusMinus() != null) {
            parser.addEnter(CPPTokenType.C_ASSIGN, ctx.getStart());
        }
    }

    @SafeVarargs
    private <T extends ParserRuleContext> T getIndirectParent(ParserRuleContext ctx, Class<T> parent, Class<? extends ParserRuleContext>... stops) {
        ParserRuleContext currentCtx = ctx;
        Set<Class<? extends ParserRuleContext>> forbidden = Set.of(stops);
        do {
            ParserRuleContext context = currentCtx.getParent();
            if (context == null) {
                return null;
            }
            if (context.getClass() == parent) {
                return parent.cast(context);
            }
            if (forbidden.contains(context.getClass())) {
                return null;
            }
            currentCtx = context;
        } while (true);
    }

    @SafeVarargs
    private boolean hasIndirectParent(ParserRuleContext ctx, Class<? extends ParserRuleContext> parent, Class<? extends ParserRuleContext>... stops) {
        return getIndirectParent(ctx, parent, stops) != null;
    }
}
