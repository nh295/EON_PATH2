it = exp.keySet.iterator;
while it.hasNext
    list = exp.get(it.next);
    for i = 0:length(list)-1
        disp(list.get(i).getSlotValue('satisfied-by'));
    end
end