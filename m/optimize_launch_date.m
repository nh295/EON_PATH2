function [data_continuity_scores,optimal_launch_date] = optimize_launch_date(r,mission,params)
PLOT = 1;
tmp_miss = mission;
lds = 2010:0.5:(params.enddate - mission.lifetime);
data_continuity_scores = zeros(1,length(lds));
for i = 1:length(lds)
    fprintf('Evaluating mission %d of %d...\n',i,length(lds));
    tmp_miss.launch_date = lds(i);
    [r,~,~,~,~,data_continuity_scores(i),~] = RBES_Evaluate_Mission(r,tmp_miss,params);
    
end
if PLOT
    title(['Data continuity score vs launch date for mission ' mission.name]);
    plot(lds,data_continuity_scores,'Color','b','Marker','x','LineStyle','-');
    xlabel('Launch date');
    ylabel('Data continuity score');
end
[~,ind] = max(data_continuity_scores);
optimal_launch_date = lds(ind);
end

