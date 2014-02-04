package br.ufes.inf.sergio;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IPOFCCNxService extends IFloodlightService {
    public int addName(String name);
    public int addCache(String name);
}
