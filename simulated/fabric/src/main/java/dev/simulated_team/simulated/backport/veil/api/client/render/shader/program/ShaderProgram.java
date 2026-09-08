package dev.simulated_team.simulated.backport.veil.api.client.render.shader.program;

/** A compiled shader program. Inert; see the package documentation. */
public class ShaderProgram {

    public static void unbind() {
    }

    public int getId() {
        return 0;
    }

    public dev.simulated_team.simulated.backport.veil.api.client.render.shader.uniform.ShaderUniform
            getUniform(final String name) {
        return new dev.simulated_team.simulated.backport.veil.api.client.render.shader.uniform.ShaderUniform();
    }

    public void setDefaultUniforms(final Object mode) {
    }

    public void setDefaultUniforms(final Object mode, final org.joml.Matrix4f modelView,
                                   final org.joml.Matrix4f projection, final Object window) {
    }

    public void bind() {
    }

    public dev.simulated_team.simulated.backport.veil.api.client.render.shader.uniform.ShaderUniform
            getUniformSafe(final String name) {
        return new dev.simulated_team.simulated.backport.veil.api.client.render.shader.uniform.ShaderUniform();
    }

    public void setFloat(final String name, final float value) {
    }

    public void setVector(final String name, final float x, final float y, final float z) {
    }
}
