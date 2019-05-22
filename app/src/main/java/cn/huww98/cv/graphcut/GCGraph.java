package cn.huww98.cv.graphcut;

import java.util.*;

enum VertexStatus {
    Free, Source, Sink
}

class GCGraph {
    /**
     * Weights for edges from Source or Sink to vertex.
     * Positive for source, negative for sink
     */
    final private double[] weights;

    final private int numEdgePerVertex;
    final private int[] edgeDestinationOffsets;

    /**
     * Weights for edges between vertices
     */
    final private double[] edgeWeights;

    private double flow = 0.0;
    final private int w;
    final private int h;

    GCGraph(int w, int h) {
        this.w = w;
        this.h = h;
        int numVertex = w * h;
        weights = new double[numVertex];
        this.edgeDestinationOffsets = new int[]{1, w};
        this.numEdgePerVertex = edgeDestinationOffsets.length * 2; // * 2 for reverse edge
        edgeWeights = new double[numVertex * numEdgePerVertex];
    }

    void addTermWeights(int vertexIndex, double sourceWeight, double sinkWeight) {
        double originalWeight = weights[vertexIndex];
        if (originalWeight > 0) {
            sourceWeight += originalWeight;
        } else {
            sinkWeight -= originalWeight;
        }
        flow += Math.min(sourceWeight, sinkWeight);
        weights[vertexIndex] = sourceWeight - sinkWeight;
    }

    void setEdgeWeights(int vertexIndex, double[] weights) {
        int baseIndex = vertexIndex * numEdgePerVertex;
        for (int i = 0; i < weights.length; i++) {
            edgeWeights[baseIndex + i * 2] = weights[i];
            edgeWeights[baseIndex + i * 2 + 1] = weights[i];
        }
    }

    private static class Neighbor {
        final int destinationIndex;
        final int edgeIndex;

        private Neighbor(int destinationIndex, int edgeIndex) {
            this.destinationIndex = destinationIndex;
            this.edgeIndex = edgeIndex;
        }
    }

    private List<Neighbor> getNeighbors(int vertexIndex) {
        ArrayList<Neighbor> result = new ArrayList<>(2 * edgeDestinationOffsets.length);
        int x = vertexIndex % w;
        int y = vertexIndex / w;

        if (x > 0) {
            int u = vertexIndex - 1;
            int edgeIndex = u * numEdgePerVertex + 1;
            result.add(new Neighbor(u, edgeIndex));
        }
        if (y > 0) {
            int u = vertexIndex - w;
            int edgeIndex = u * numEdgePerVertex + 3;
            result.add(new Neighbor(u, edgeIndex));
        }
        if (x < w - 1) {
            int u = vertexIndex + 1;
            int edgeIndex = vertexIndex * numEdgePerVertex;
            result.add(new Neighbor(u, edgeIndex));
        }
        if (y < h - 1) {
            int u = vertexIndex + w;
            int edgeIndex = vertexIndex * numEdgePerVertex + 2;
            result.add(new Neighbor(u, edgeIndex));
        }
        return result;
    }

    private int[] getEdgeEnds(int edgeIndex) {
        int u = edgeIndex / numEdgePerVertex;
        int i = edgeIndex % numEdgePerVertex;
        int v = u + edgeDestinationOffsets[i / 2];
        return new int[]{u, v};
    }

    private int getEdgeFrom(int edgeIndex) {
        int u = edgeIndex / numEdgePerVertex;
        if (edgeIndex % 2 == 0) {
            return u;
        } else {
            int i = edgeIndex % numEdgePerVertex;
            return u + edgeDestinationOffsets[i / 2];
        }
    }

    private int getReverseEdge(int edgeIndex) {
        return edgeIndex ^ 1;
    }

    VertexStatus[] noop() {
        VertexStatus[] status = new VertexStatus[weights.length];
        for (int i = 0; i < weights.length; i++) {
            status[i] = weights[i] > 0 ? VertexStatus.Source : VertexStatus.Sink;
        }
        return status;
    }

    VertexStatus[] maxFlow() {
        Queue<Integer> active = new ArrayLinkedList(weights.length);
        VertexStatus[] status = new VertexStatus[weights.length];
        int[] parent = new int[weights.length]; // Index of edge from parent to this vertex
//        int[] depth = new int[weights.length];
        final int TERMINAL = -1, ORPHAN = -2;

        for (int i = 0; i < weights.length; i++) {
            double weight = weights[i];
            if (weight != 0) {
                active.offer(i);
                parent[i] = TERMINAL;
                status[i] = weight > 0 ? VertexStatus.Source : VertexStatus.Sink;
//                depth[i] = 1;
            } else {
                parent[i] = ORPHAN;
                status[i] = VertexStatus.Free;
//                depth[i] = -1;
            }
        }

        while (true) {
            // Growth Stage
            int contactEdgeIndex = growthTree(active, status, parent);

            if (contactEdgeIndex < 0) {
                break;
            }
//            System.out.println("contact: " + contactEdgeIndex + ", active count: " + active.size());

            // Augmentation Stage
            double minWeight = edgeWeights[contactEdgeIndex];
            assert minWeight > 0;
            for (int i : getEdgeEnds(contactEdgeIndex)) {
                int j = i;
                while (parent[j] >= 0) {
                    minWeight = Math.min(minWeight, edgeWeights[parent[j]]);
                    assert minWeight > 0;
                    j = getEdgeFrom(parent[j]);
                }
                assert parent[j] == TERMINAL;
                minWeight = Math.min(minWeight, Math.abs(weights[j]));
                assert minWeight > 0;
            }

            ArrayList<Integer> orphans = new ArrayList<>();
            edgeWeights[contactEdgeIndex] -= minWeight;
            edgeWeights[getReverseEdge(contactEdgeIndex)] += minWeight;
            for (int i : getEdgeEnds(contactEdgeIndex)) {
                int currentVertex = i;
                while (true) {
                    int e = parent[currentVertex];
                    assert e != ORPHAN;
                    if (e < 0) {
                        break;
                    }
                    edgeWeights[e] -= minWeight;
                    edgeWeights[getReverseEdge(e)] += minWeight;
                    if (edgeWeights[e] == 0.0) {
                        parent[currentVertex] = ORPHAN;
                        orphans.add(currentVertex);
                    }
                    currentVertex = getEdgeFrom(e);
                }
                if (status[currentVertex] == VertexStatus.Source) {
                    weights[currentVertex] -= minWeight;
                } else {
                    weights[currentVertex] += minWeight;
                }
                if (weights[currentVertex] == 0) {
                    parent[currentVertex] = ORPHAN;
                    orphans.add(currentVertex);
                }
            }

            // Adoption Stage
            while (!orphans.isEmpty()) {
                int o = orphans.get(orphans.size() - 1);
                orphans.remove(orphans.size() - 1);

                Neighbor newParent = null;
                int minDepth = Integer.MAX_VALUE;
                for (Neighbor n : getNeighbors(o)) {
                    if (status[o] != status[n.destinationIndex] ||
                            edgeWeights[getReverseEdge(n.edgeIndex)] == 0) {
                        continue;
                    }
                    int currentDepth = 1;
                    int v = n.destinationIndex;
                    while (parent[v] >= 0) {
                        currentDepth++;
                        v = getEdgeFrom(parent[v]);
                    }
                    if (parent[v] == TERMINAL && currentDepth < minDepth) {
                        newParent = n;
                        minDepth = currentDepth;
                    }
                }

                if (newParent != null) {
                    parent[o] = getReverseEdge(newParent.edgeIndex);
                } else {
                    for (Neighbor n : getNeighbors(o)) {
                        if (status[o] != status[n.destinationIndex]) {
                            continue;
                        }
                        if (edgeWeights[n.edgeIndex] > 0) {
                            active.add(n.destinationIndex);
                        }
                        if (parent[n.destinationIndex] == n.edgeIndex) {
                            parent[n.destinationIndex] = ORPHAN;
                            orphans.add(n.destinationIndex);
                        }
                    }
                    status[o] = VertexStatus.Free;
                    parent[o] = ORPHAN;
                    // Not remove o from active, it is to slow.
                }
            }
        }

        return status;
    }

    /**
     * @return Edge index from source to sink
     */
    private int growthTree(Queue<Integer> active, VertexStatus[] status, int[] parent) {
        while (!active.isEmpty()) {
            int v = active.element();
            VertexStatus vs = status[v];
            if (vs != VertexStatus.Free) { // Maybe v is an orphan
                for (Neighbor n : getNeighbors(v)) {
                    double edgeWeight = edgeWeights[n.edgeIndex];
                    if (edgeWeight == 0.0) {
                        continue;
                    }
                    int u = n.destinationIndex;
                    VertexStatus us = status[u];
                    if (us == VertexStatus.Free) {
                        status[u] = vs;
                        parent[u] = n.edgeIndex;
                        active.offer(u);
                    } else if (us != vs) {
                        int contactEdge = vs == VertexStatus.Source ? n.edgeIndex : getReverseEdge(n.edgeIndex);
                        if (edgeWeights[contactEdge] > 0) {
                            return contactEdge;
                        }
                    }
                }
            }
            active.remove();
        }
        return -1;
    }
}
